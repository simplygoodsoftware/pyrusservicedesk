package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import androidx.core.net.toFile
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.Application
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.Member
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchResult
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Status
import com.pyrus.pyrusservicedesk._ref.utils.GetTicketsError
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.Try2
import com.pyrus.pyrusservicedesk._ref.utils.isFailed
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk._ref.utils.toTry2
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getInstanceId
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.DatabaseMapper
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComments
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap


internal class SdRepository(
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
        coroutineScope.launch(Dispatchers.IO) {
            val initialCommands = commandsStore.getCommands()
                .filter { !it.command.isError }
                .mapNotNull(repositoryMapper::mapToSyncRequest)

            if (initialCommands.isNotEmpty()) {
                for (command in initialCommands) {
                    sendCommand(command)
                }
            }
            for (command in initialCommands) {
                sendCommand(command)
            }
        }
    }

    suspend fun sync() {
        synchronizer.syncData(SyncRequest.Data, false)
    }

    fun getTicketsWithComments(): List<TicketWithComments> {
        return ticketsStore.getTicketsWithComments()
    }

    suspend fun getTicketsInfo(force: Boolean): Try<TicketsInfo> {
        val account = accountStore.getAccount()
        if (!force) {
            val applications = ticketsStore.getApplicationsWithTickets()
            if (applications.isNotEmpty()) {
                val tickets = repositoryMapper.mergeData(account, applications, commandsStore.getCommands())
                return Try.Success(tickets)
            }
        }

        val syncTry = synchronizer.syncData(SyncRequest.Data, force)
        if (!syncTry.isSuccess()) return syncTry
        val applications = ticketsStore.getApplicationsWithTickets()

        val tickets = repositoryMapper.mergeData(account, applications, commandsStore.getCommands())
        return Try.Success(tickets)
    }

    fun getTicketsInfoFlow(): Flow<TicketsInfo> = combine(
        accountStore.accountStateFlow(),
        ticketsStore.getApplicationsWithTicketsFlow(),
        commandsStore.getCommandsFlow(),
    ) { account, applications, commands ->
        repositoryMapper.mergeData(account, applications, commands)
    }

    suspend fun getFeed(userId: String, ticketId: Long, force: Boolean): Try2<FullTicket, GetTicketsError> {
        val account = accountStore.getAccount()
        val serverId = idStore.getTicketServerId(ticketId) ?: ticketId
        val orgLogoUrl = getOrgLogoUrl(userId, account)

        if (serverId <= 0) {
            val commands = commandsStore.getCommands(serverId)
            val firstCommand = commands.firstOrNull()
            val ticket = if (firstCommand != null) {
                repositoryMapper.mapToFullTicket(
                    ticketId = firstCommand.command.ticketId!!,
                    userId = firstCommand.command.userId ?: account.getInstanceId(),
                    addCommentCommands = commands,
                    orgLogoUrl = orgLogoUrl
                )
            }
            else {
                FullTicket(
                    subject = "",
                    comments = emptyList(),
                    showRating = false,
                    showRatingText = null,
                    userId = userId,
                    ticketId = ticketId,
                    orgLogoUrl = orgLogoUrl,
                    isActive = true,
                    isRead = true,
                    ratingSettings = null,
                )
            }
            return Try2.Success(ticket)
        }

        if (!force) {
            val localTicket = ticketsStore.getTicketWithComments(serverId)
            val commands = commandsStore.getCommands(serverId)
            if (localTicket != null) {
                val ticket = repositoryMapper.mergeTicket(
                    account,
                    userId,
                    localTicket,
                    commands,
                    orgLogoUrl
                )
                return Try.Success(ticket).toTry2()
            }
        }

        val syncTry = synchronizer.syncData(SyncRequest.Data, force).checkResponse(userId)
        if (syncTry.isFailed()) return syncTry
        val localTicket = ticketsStore.getTicketWithComments(301863225)
            ?: return Try2.Failure(GetTicketsError.NoDataFound)

        val commands = commandsStore.getCommands(serverId)
        val ticket = repositoryMapper.mergeTicket(account, userId, localTicket, commands, orgLogoUrl)
        return Try2.Success(ticket)
    }

    fun getFeedFlow(user: UserInternal, ticketId: Long): Flow<FullTicket?> {
        return combine(
            accountStore.accountStateFlow(),
            ticketsStore.getTicketWithCommentsFlow(ticketId),
            commandsStore.getCommandsFlow(ticketId),
        ) { account, ticketEntity, commands ->

            val orgLogoUrl = getOrgLogoUrl(user.userId, account)
            if (ticketEntity != null) {
                repositoryMapper.mergeTicket(
                    account,
                    ticketEntity.ticket.userId,
                    ticketEntity,
                    commands,
                    orgLogoUrl
                )
            }
            else {
                val firstCommand = commands.firstOrNull()
                if (firstCommand != null) {
                    repositoryMapper.mapToFullTicket(
                        ticketId = firstCommand.command.ticketId!!,
                        userId = firstCommand.command.userId ?: account.getInstanceId(),
                        addCommentCommands = commands,
                        orgLogoUrl = orgLogoUrl
                    )
                }
                else {
                    FullTicket(
                        subject = "",
                        comments = emptyList(),
                        showRating = false,
                        showRatingText = null,
                        userId = user.userId,
                        ticketId = ticketId,
                        orgLogoUrl = orgLogoUrl,
                        isActive = true,
                        isRead = true,
                        ratingSettings = null,
                    )
                }
            }
        }
    }

    fun addTextComment(user: UserInternal, ticketId: Long, textBody: String) = coroutineScope.launch(Dispatchers.IO) {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val instanceId = accountStore.getAccount().getInstanceId()
        val command: SyncRequest.Command.CreateComment = commandsStore.addTextCommand(user, serverTicketId, textBody, instanceId)
        sendCommand(command)
    }

    fun addAttachComment(user: UserInternal, ticketId: Long, fileUri: Uri) = coroutineScope.launch(Dispatchers.IO) {
        val fileData = fileResolver.getFileData(fileUri) ?: return@launch

        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val instanceId = accountStore.getAccount().getInstanceId()
        val commandEntity = commandsStore.addAttachmentCommand(user, serverTicketId, fileData, instanceId)

        val command = repositoryMapper.mapToSyncRequest(commandEntity) ?: return@launch
        sendCommand(command)
    }

    fun addRatingComment(user: UserInternal, ticketId: Long, rating: Int?, ratingComment: String?) = coroutineScope.launch(Dispatchers.IO) {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val instanceId = accountStore.getAccount().getInstanceId()
        val command = commandsStore.addRatingCommand(user, serverTicketId, rating, ratingComment, instanceId)
        sendCommand(command)
    }

    fun readTicket(user: UserInternal, ticketId: Long) = coroutineScope.launch(Dispatchers.IO) {
        if (ticketId <= 0) return@launch
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val instanceId = accountStore.getAccount().getInstanceId()
        val command = commandsStore.addReadCommand(user, serverTicketId, instanceId)
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
        val instanceId = accountStore.getAccount().getInstanceId()
        val command = commandsStore.createPushTokenCommand(user, token, tokenType, instanceId)
        return synchronizer.syncCommand(command).map {  }
    }

    fun retryAddComment(user: UserInternal, localId: Long) = coroutineScope.launch(Dispatchers.IO) {
        val commandEntity = commandsStore.getCommand(localId) ?: return@launch
        val command = repositoryMapper.mapToSyncRequest(commandEntity) ?: return@launch
        val instanceId = accountStore.getAccount().getInstanceId()
        commandsStore.addOrUpdatePendingCommand(repositoryMapper.mapToCommandEntity(false, command, instanceId))

        if (command is SyncRequest.Command.CreateComment) {
            val serverTicketId = idStore.getTicketServerId(command.ticketId) ?: command.ticketId
            sendCommand(command.copy(ticketId = serverTicketId))
        }
        else {
            sendCommand(command)
        }
    }

    fun removeCommand(localId: Long) {
        commandsStore.removeCommand(localId)
    }

    fun cancelUploadFile(commandId: Long, attachmentId: Long) {
        removeCommand(commandId)
        val hook = fileHooks[attachmentId]
        hook?.cancelUploading()
        fileHooks.remove(attachmentId)
    }

    suspend fun searchTickets(text: String, limit: Int): List<SearchResult> {
        val account = accountStore.getAccount()
        val tickets = ticketsStore.searchTickets(text, limit).mapNotNull {
            repositoryMapper.mapToSearchResult(account, it)
        }
        val localCommands = commandsStore.searchComments(text, limit).mapNotNull {
            repositoryMapper.mapToSearchResult(account, it)
        }

        val result = tickets + localCommands
        return result.distinctBy { it.commentId }.sortedByDescending { it.creationTime }
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
            val instanceId = accountStore.getAccount().getInstanceId()
            commandsStore.addOrUpdatePendingCommand(repositoryMapper.mapToCommandEntity(isError, newCommand, instanceId))
        }

        for (attachment in attachments) {
            val uploadTry = sendAttachment(attachment) { progress ->
                val newAttachment = attachment.copy(progress = progress)
                updateAttachment(isError = false, attachment = newAttachment)
            }
            when(uploadTry) {
                is Try.Failure -> {
                    val newAttachment = attachment.copy(status = Status.Error, progress = null)
                    updateAttachment(isError = true, attachment = newAttachment)
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

    fun getApplications(): List<Application> {
        return ticketsStore.getApplications().map(DatabaseMapper::mapToApplication)
    }

    fun getMembers(userId: String): List<Member> {
        return ticketsStore.getMembersByUserId(userId).map(DatabaseMapper::mapToMember)
    }

    private fun getOrgLogoUrl(userId: String, account: Account): String? {
        val appId = account.getUsers().find { it.userId == userId }?.appId ?: return null
        val applications = ticketsStore.getApplications()
        val orgLogoUrl = applications.find { it.appId == appId }?.orgLogoUrl ?: return null
        return RequestUtils.getOrganisationLogoUrl(orgLogoUrl, account.domain)
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
            val instanceId = accountStore.getAccount().getInstanceId()
            val errorEntity = repositoryMapper.mapToCommandEntity(true, command, instanceId)
            commandsStore.addOrUpdatePendingCommand(errorEntity)
        }
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