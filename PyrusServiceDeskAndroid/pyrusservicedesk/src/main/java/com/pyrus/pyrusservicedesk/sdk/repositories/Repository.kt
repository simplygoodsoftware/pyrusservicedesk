package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import androidx.core.net.toFile
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.GetTicketsError
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.Try2
import com.pyrus.pyrusservicedesk._ref.utils.isFailed
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk._ref.utils.toTry2
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
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
    private val localCommandsStore: LocalCommandsStore,
    private val repositoryMapper: RepositoryMapper,
    private val fileResolver: FileResolver,
    private val remoteFileStore: RemoteFileStore,
    private val synchronizer: Synchronizer,
    private val localTicketsStore: LocalTicketsStore,
    private val coroutineScope: CoroutineScope,
    private val accountStore: AccountStore,
    private val idStore: IdStore,
) {

    private val fileHooks = ConcurrentHashMap<Long, UploadFileHook>()

    suspend fun getAllData(force: Boolean): Try<TicketsInfo> {
        if (!force) {
            val localTickets: TicketsDto? = localTicketsStore.getTickets()
            if (localTickets != null) {
                return Try.Success(mergeData(localTickets, localCommandsStore.getCommands()))
            }
        }
        return synchronizer.syncData(SyncRequest.Data).map {
            mergeData(it, localCommandsStore.getCommands())
        }
    }

    fun getAllDataFlow(): Flow<TicketsInfo?> = combine(
        localTicketsStore.getTicketInfoFlow(),
        localCommandsStore.getCommandsFlow(),
    ) { dto, commands ->
        mergeData(dto, commands)
    }

    //TODO what we need to do when we haven't fount ticket (feedTry.value == null, but feedTry.isSuccess()) (k) sm
    suspend fun getFeed(
        ticketId: Long,
        force: Boolean,
    ): Try2<FullTicket, GetTicketsError> {

        if (!force) {
            val localTickets: TicketsDto? = localTicketsStore.getTickets()
            val ticketDto: TicketDto? = localTickets?.tickets?.find { it.ticketId == ticketId }
            val commands = localCommandsStore.getCommands(ticketId)
            val ticket = ticketDto?.let {
                ticketDto.userId?.let { userId ->
                    repositoryMapper.mergeTicket(userId, ticketDto, commands)
                }
            }
            if (ticket != null) {
                return Try.Success(ticket).toTry2()
            }
        }

        val syncTry = synchronizer.syncData(SyncRequest.Data).checkResponse(ticketId)
        if (syncTry.isFailed()) return syncTry
        val ticketDto = syncTry.value
        val userId = ticketDto.userId ?: return Try2.Failure(GetTicketsError.ValidationError)

        val commands = localCommandsStore.getCommands(ticketId)
        val ticket = repositoryMapper.mergeTicket(userId, ticketDto, commands)
        return Try2.Success(ticket)
    }

    fun getFeedFlow(ticketId: Long): Flow<FullTicket?> {
        return combine(
            localTicketsStore.getTicketInfoFlow(ticketId),
            localCommandsStore.getCommandsFlow(ticketId),
        ) { ticketDto, commands ->
            if (ticketDto != null) {
                if (ticketDto.userId == null) return@combine null
                repositoryMapper.mergeTicket(ticketDto.userId, ticketDto, commands)
            }
            else {
                commands.find { it.requestNewTicket == true }?.let {
                    repositoryMapper.mapToFullTicket(it.ticketId!!, it.userId, commands)
                }
            }
        }
    }

    fun addTextComment(
        user: UserInternal,
        ticketId: Long,
        textBody: String,
    ) = coroutineScope.launch {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val requestNewTicket = needsToRequestNewTicket(serverTicketId)
        val command = localCommandsStore.addTextCommand(user, serverTicketId, requestNewTicket, textBody)
        syncCommand(command)
    }

    fun addAttachComment(
        user: UserInternal,
        ticketId: Long,
        fileUri: Uri,
    ) = coroutineScope.launch {
        val fileData = fileResolver.getFileData(fileUri) ?: return@launch

        var serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        var requestNewTicket = needsToRequestNewTicket(serverTicketId)

        val commandEntity = localCommandsStore.addAttachmentCommand(user, ticketId, requestNewTicket, fileData)
        val attachmentEntity = commandEntity.attachments!!.first()

        val file = fileData.uri.toFile()
        val hook = UploadFileHook()
        fileHooks[attachmentEntity.id] = hook

        val uploadTry = remoteFileStore.uploadFile(file, hook) { progress ->
            val newAttachmentEntity = attachmentEntity.copy(progress = progress)
            val newCommandEntity = commandEntity.copy(attachments = listOf(newAttachmentEntity))
            localCommandsStore.addOrUpdatePendingCommand(newCommandEntity)
        }

        val newLocalAttachment = when {
            uploadTry.isSuccess() -> attachmentEntity.copy(
                guid = uploadTry.value.guid,
                status = Status.Completed.ordinal,
                progress = null,
            )
            else -> attachmentEntity.copy(status = Status.Error.ordinal, progress = null)
        }
        val newCommandEntity = commandEntity.copy(attachments = listOf(newLocalAttachment))

        localCommandsStore.addOrUpdatePendingCommand(newCommandEntity)
        fileHooks.remove(attachmentEntity.id)

        serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        requestNewTicket = needsToRequestNewTicket(serverTicketId)

        val command = SyncRequest.Command.CreateComment(
            localId = commandEntity.localId,
            commandId = commandEntity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = commandEntity.creationTime,
            requestNewTicket = requestNewTicket,
            ticketId = serverTicketId,
            comment = null,
            attachments = listOf(repositoryMapper.map(newLocalAttachment)),
            rating = null,
        )

        syncCommand(command)
    }

    fun addRatingComment(
        user: UserInternal,
        ticketId: Long,
        rating: Int,
    ) = coroutineScope.launch {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val requestNewTicket = needsToRequestNewTicket(serverTicketId)
        val command = localCommandsStore.addRatingCommand(user, serverTicketId, requestNewTicket, rating)
        syncCommand(command)
    }

    fun readTicket(user: UserInternal, ticketId: Long) = coroutineScope.launch {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val command = localCommandsStore.addReadCommand(user, serverTicketId)
        val syncTry = synchronizer.syncCommand(command)
        if (syncTry.isSuccess()) {
            localCommandsStore.removeCommand(command.commandId)
        }
    }

    fun retryAddComment(user: UserInternal, commandId: String) {

        // TODO
//        val command = localCommandsStore.getCommandError(commandId) ?: return
//        val text = (command.params as? CommandParamsDto.CreateComment)?.comment
//        when {
//            //!localComment.attachments.isNullOrEmpty() -> addAttachComment(localComment.attachments.first().uri) //TODO
//            !text.isNullOrBlank() -> addTextComment(user, ticketId, text)
//            localComment.rating != null -> addRatingComment(localComment.rating) //TODO
//            else -> localCommandsStore.removePendingCommand(command)
//        }
    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(user: UserInternal, token: String, tokenType: String): Try<Unit> {
        val command = localCommandsStore.createPushTokenCommand(user, token, tokenType)
        return synchronizer.syncCommand(command).map {  }
    }

    fun removeCommand(commandId: String) {
        localCommandsStore.removeCommand(commandId)
    }

    private fun needsToRequestNewTicket(ticketId: Long): Boolean {
        return ticketId <= 0
    }

    private suspend fun syncCommand(command: SyncRequest.Command) {
        val syncTry = synchronizer.syncCommand(command)

        if (syncTry.isSuccess()) {
            localCommandsStore.removeCommand(command.commandId)
        }
        else {
            val errorEntity = repositoryMapper.mapToCommandErrorEntity(command)
            localCommandsStore.addOrUpdatePendingCommand(errorEntity)
        }
    }

    private fun mergeData(
        ticketsDto: TicketsDto?,
        commands: List<CommandEntity>,
    ): TicketsInfo {
        return TicketsInfo(repositoryMapper.mergeTickets(ticketsDto, commands))
    }

    private fun Try<TicketsDto>.checkResponse(ticketId: Long): Try2<TicketDto, GetTicketsError> {
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
        val ticket: TicketDto? = tickets.tickets?.find { it.ticketId == ticketId }
        if (ticket == null) {
            return Try2.Failure(GetTicketsError.NoDataFound)
        }
        val res = Try.Success(ticket)
        return res.toTry2()

    }


}