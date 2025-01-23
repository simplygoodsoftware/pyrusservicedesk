package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import androidx.core.net.toFile
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.GetTicketsError
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.Try2
import com.pyrus.pyrusservicedesk._ref.utils.isFailed
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk._ref.utils.toTry2
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap


internal class Repository(
    private val commandsStore: LocalCommandsStore,
    private val repositoryMapper: RepositoryMapper,
    private val fileResolver: FileResolver,
    private val remoteFileStore: RemoteFileStore,
    private val synchronizer: Synchronizer,
    private val ticketsStore: LocalTicketsStore,
    private val coroutineScope: CoroutineScope,
    private val accountStore: AccountStore,
    private val idStore: IdStore,
) {

    private val fileHooks = ConcurrentHashMap<Long, UploadFileHook>()

    init {
        val initialCommands = commandsStore.getCommands()
            .filter { !it.isError }
            .mapNotNull(repositoryMapper::mapToSyncRequest)

        if (initialCommands.isNotEmpty()) {
            coroutineScope.launch {
                for (command in initialCommands) {
                    sendCommand(command)
                }
            }
        }
    }

    suspend fun getAllData(force: Boolean): Try<TicketsInfo> {
        val account = accountStore.getAccount()
        if (!force) {
            val localTickets: TicketsDto? = ticketsStore.getTickets()
            if (localTickets != null) {
                return Try.Success(mergeData(account, localTickets, commandsStore.getCommands()))
            }
        }
        return synchronizer.syncData(SyncRequest.Data).map {
            mergeData(account, it, commandsStore.getCommands())
        }
    }

    fun getAllDataFlow(): Flow<TicketsInfo> = combine(
        accountStore.accountStateFlow(),
        ticketsStore.getTicketInfoFlow(),
        commandsStore.getCommandsFlow(),
        ::mergeData
    )

    private fun getOrgLogoUrl(userId: String, account: Account): String? {
        val appId = account.getUsers().find { it.userId == userId }?.appId ?: return null
        val applications = ticketsStore.getTickets()?.applications ?: return null
        val orgLogoUrl = applications.find { it.appId == appId }?.orgLogoUrl ?: return null
        return RequestUtils.getOrganisationLogoUrl(orgLogoUrl, account.domain)
    }

    suspend fun getFeed(userId: String, ticketId: Long, force: Boolean): Try2<FullTicket, GetTicketsError> {
        val account = accountStore.getAccount()
        val serverId = idStore.getTicketServerId(ticketId) ?: ticketId

        val orgLogoUrl = getOrgLogoUrl(userId, account)

        if (serverId < 0) {
            val commands = commandsStore.getCommands(serverId)
            val firstCommand = commands.firstOrNull()
            val ticket = if (firstCommand != null) {
                repositoryMapper.mapToFullTicket(firstCommand.ticketId!!, firstCommand.userId, commands, orgLogoUrl)
            }
            else {
                FullTicket(
                    subject = "",
                    isRead = true,
                    lastComment = null,
                    comments = emptyList(),
                    showRating = false,
                    showRatingText = null,
                    isActive = true,
                    userId = userId,
                    ticketId = ticketId,
                    orgLogoUrl = orgLogoUrl
                )
            }
            return Try2.Success(ticket)
        }

        if (!force) {
            val localTickets: TicketsDto? = ticketsStore.getTickets()
            val ticketDto: TicketDto? = localTickets?.tickets?.find { it.ticketId == serverId }
            val commands = commandsStore.getCommands(serverId)
            if (ticketDto != null) {
                val ticket = repositoryMapper.mergeTicket(
                    account,
                    userId,
                    ticketDto,
                    commands,
                    orgLogoUrl
                )
                return Try.Success(ticket).toTry2()
            }
        }

        val syncTry = synchronizer.syncData(SyncRequest.Data).checkResponse(userId)
        if (syncTry.isFailed()) return syncTry
        val ticketsDto = syncTry.value

        val ticketDto = ticketsDto.tickets?.find { it.ticketId == ticketId } ?: ticketsStore.getTicket(ticketId)
        if (ticketDto == null) {
            return Try2.Failure(GetTicketsError.NoDataFound)
        }

        val commands = commandsStore.getCommands(serverId)
        val ticket = repositoryMapper.mergeTicket(account, userId, ticketDto, commands, orgLogoUrl)
        return Try2.Success(ticket)
    }

    fun getFeedFlow(user: UserInternal, ticketId: Long): Flow<FullTicket?> {
        return combine(
            accountStore.accountStateFlow(),
            ticketsStore.getTicketInfoFlow(ticketId),
            commandsStore.getCommandsFlow(ticketId),
        ) { account, ticketDto, commands ->

            val orgLogoUrl = getOrgLogoUrl(user.userId, account)
            if (ticketDto != null) {
                if (ticketDto.userId == null) return@combine null
                repositoryMapper.mergeTicket(
                    account,
                    ticketDto.userId,
                    ticketDto,
                    commands,
                    orgLogoUrl
                )
            }
            else {
                val firstCommand = commands.firstOrNull()
                if (firstCommand != null) {
                    repositoryMapper.mapToFullTicket(firstCommand.ticketId!!, firstCommand.userId, commands, orgLogoUrl)
                }
                else {
                    FullTicket(
                        subject = "",
                        isRead = true,
                        lastComment = null,
                        comments = emptyList(),
                        showRating = false,
                        showRatingText = null,
                        isActive = true,
                        userId = user.userId,
                        ticketId = ticketId,
                        orgLogoUrl = orgLogoUrl
                    )
                }
            }
        }
    }

    fun addTextComment(user: UserInternal, ticketId: Long, textBody: String) = coroutineScope.launch {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val requestNewTicket = needsToRequestNewTicket(serverTicketId)
        val command: SyncRequest.Command.CreateComment = commandsStore.addTextCommand(user, serverTicketId, requestNewTicket, textBody)
        sendCommand(command)
    }

    fun addAttachComment(user: UserInternal, ticketId: Long, fileUri: Uri) = coroutineScope.launch {
        val fileData = fileResolver.getFileData(fileUri) ?: return@launch

        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val requestNewTicket = needsToRequestNewTicket(serverTicketId)
        val commandEntity = commandsStore.addAttachmentCommand(user, ticketId, requestNewTicket, fileData)

        val command = repositoryMapper.mapToSyncRequest(commandEntity) ?: return@launch
        sendCommand(command)
    }

    fun addRatingComment(user: UserInternal, ticketId: Long, rating: Int) = coroutineScope.launch {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val requestNewTicket = needsToRequestNewTicket(serverTicketId)
        val command = commandsStore.addRatingCommand(user, serverTicketId, requestNewTicket, rating)
        sendCommand(command)
    }

    fun readTicket(user: UserInternal, ticketId: Long) = coroutineScope.launch {
        if (ticketId <= 0) return@launch
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val command = commandsStore.addReadCommand(user, serverTicketId)
        val syncTry = synchronizer.syncCommand(command)
        if (syncTry.isSuccess()) {
            commandsStore.removeCommand(command.commandId)
        }
    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(user: UserInternal, token: String, tokenType: String): Try<Unit> {
        val command = commandsStore.createPushTokenCommand(user, token, tokenType)
        return synchronizer.syncCommand(command).map {  }
    }

    fun retryAddComment(user: UserInternal, localId: Long) = coroutineScope.launch {
        val commandEntity = commandsStore.getCommand(localId) ?: return@launch
        val command = repositoryMapper.mapToSyncRequest(commandEntity) ?: return@launch
        commandsStore.addOrUpdatePendingCommand(repositoryMapper.mapToCommandEntity(false, command))

        if (command is SyncRequest.Command.CreateComment) {
            val serverTicketId = idStore.getTicketServerId(command.ticketId) ?: command.ticketId
            val requestNewTicket = needsToRequestNewTicket(serverTicketId)
            sendCommand(command.copy(ticketId = serverTicketId, requestNewTicket = requestNewTicket))
        }
        else {
            sendCommand(command)
        }
    }

    fun removeCommand(localId: Long) {
        commandsStore.removeCommand(localId)
    }

    fun cancelUploadFile(attachmentId: Long) {
        val hook = fileHooks[attachmentId]
        hook?.cancelUploading()
        fileHooks.remove(attachmentId)
    }

    private fun needsToRequestNewTicket(ticketId: Long): Boolean {
        return ticketId <= 0
    }

    private suspend fun sendCommand(command: SyncRequest.Command) {
        val sendFilesTry = sendAttachments(command)
        if (!sendFilesTry.isSuccess()) return
        syncCommand(sendFilesTry.value)
    }

    private suspend fun sendAttachments(command: SyncRequest.Command): Try<SyncRequest.Command> {
        if (command !is SyncRequest.Command.CreateComment) return Try.Success(command)
        val attachments = command.attachments?.filter { it.guid == null }
        if (attachments.isNullOrEmpty()) return Try.Success(command)

        var resultAttachments: List<Attachment> = attachments

        fun updateAttachment(isError: Boolean, attachment: Attachment) {
            resultAttachments = resultAttachments.map {
                if (it.id == attachment.id) attachment
                else it
            }

            val newCommand = command.copy(attachments = attachments)
            commandsStore.addOrUpdatePendingCommand(repositoryMapper.mapToCommandEntity(isError, newCommand))
        }

        for (attachment in attachments) {
            val uploadTry = sendAttachment(attachment) { progress ->
                val newAttachment = attachment.copy(progress = progress)
                updateAttachment(isError = false, attachment = newAttachment)
            }
            when(uploadTry) {
                is Try.Failure -> {
                    val newAttachment = attachment.copy(status = Status.Error, progress = null)
                    updateAttachment(true, newAttachment)
                    return uploadTry
                }
                is Try.Success -> {
                    val newAttachment = attachment.copy(
                        guid = uploadTry.value.guid,
                        status = Status.Completed,
                        progress = null,
                    )
                    updateAttachment(isError = false, attachment = newAttachment)
                }
            }
        }
        return Try.Success(command.copy(attachments = resultAttachments))
    }

    private suspend fun sendAttachment(
        attachment: Attachment,
        progressListener: (Int) -> Unit,
    ): Try<FileUploadResponseData> {
        val file = attachment.uri.toFile()
        val hook = UploadFileHook()
        fileHooks[attachment.id] = hook

        val uploadTry = remoteFileStore.uploadFile(file, hook, progressListener)

        fileHooks.remove(attachment.id)
        return uploadTry
    }

    private suspend fun syncCommand(command: SyncRequest.Command) {
        val syncTry = synchronizer.syncCommand(command)

        if (syncTry.isSuccess()) {
            commandsStore.removeCommand(command.commandId)
        }
        else {
            val errorEntity = repositoryMapper.mapToCommandEntity(true, command)
            commandsStore.addOrUpdatePendingCommand(errorEntity)
        }
    }

    private fun mergeData(
        account: Account,
        ticketsDto: TicketsDto?,
        commands: List<CommandEntity>,
    ): TicketsInfo {
        return TicketsInfo(
            account = account,
            ticketSetInfoList = repositoryMapper.mergeTickets(account, ticketsDto, commands)
        )
    }

    private fun Try<TicketsDto>.checkResponse(userId: String): Try2<TicketsDto, GetTicketsError> {
        if (!this.isSuccess()) {
            return this.toTry2 {
                when (error) {
                    is UnknownHostException,
                    is HttpException,
                    is InterruptedIOException,
                        -> GetTicketsError.ConnectionError
                    else -> GetTicketsError.ServiceError(
                        error.javaClass.simpleName,
                        error.message ?: "Cannot get stacktrace"
                    )
                }
            }
        }

        val tickets: TicketsDto = this.value

        if (tickets.authorAccessDenied?.find { it == userId } != null) {
            return Try2.Failure(GetTicketsError.AuthorAccessDenied)
        }

        val res = Try.Success(tickets)
        return res.toTry2()
    }

}