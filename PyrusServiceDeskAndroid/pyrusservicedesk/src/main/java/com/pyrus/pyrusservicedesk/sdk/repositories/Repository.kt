package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import androidx.core.net.toFile
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.GetTicketsError
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.Try2
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
import kotlinx.coroutines.flow.map
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
) {

    private val fileHooks = ConcurrentHashMap<Long, UploadFileHook>()

    suspend fun getAllData(force: Boolean): Try<TicketsInfo> {
        // TODO merge with sync command
        if (!force) {
            val localTickets: TicketsDto? = localTicketsStore.getTickets()
            if(localTickets != null) {
                return Try.Success(repositoryMapper.map(localTickets))
            }
        }
        return synchronizer.syncData(SyncRequest.Data).map(repositoryMapper::map)
    }

    // TODO merge with sync command and with commandErrors
    fun getAllDataFlow(): Flow<TicketsInfo?> = localTicketsStore.getTicketInfoFlow().map { dto ->
        dto?.let(repositoryMapper::map)
    }

    // TODO check it
    private fun <T : TicketsDto> Try<T>.checkResponse(ticketId: Long): Try2<FullTicket, GetTicketsError> {
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

        val res = Try.Success(repositoryMapper.map(ticket))

        return res.toTry2()

    }

    //TODO what we need to do when we haven't fount ticket (feedTry.value == null, but feedTry.isSuccess()) (k) sm
    suspend fun getFeed(
        user: UserInternal,
        ticketId: Long,
        force: Boolean,
    ): Try2<FullTicket, GetTicketsError> {

        if (!force) {
            val localTickets: TicketsDto? = localTicketsStore.getTickets()
            val ticket: TicketDto? = localTickets?.tickets?.find { it.ticketId == ticketId }
            if (ticket != null) {
                return Try.Success(repositoryMapper.map(ticket)).toTry2()
            }
        }

        val syncTry = synchronizer.syncData(SyncRequest.Data).checkResponse(ticketId)

        return syncTry
    }

    // TODO merge with sync command
    fun getFeedFlow(user: UserInternal, ticketId: Long): Flow<FullTicket?> = localTicketsStore.getTicketInfoFlow().map { ticketsDto ->
        ticketsDto?.tickets
            ?.find { ticketDto -> ticketDto.ticketId == ticketId }
            ?.let(repositoryMapper::map)
    }

    fun addTicket(user: UserInternal, textBody: String) = coroutineScope.launch {
        val command = localCommandsStore.addCreateTicketCommand(user, textBody)
        syncCommand(command)
    }

    fun addTextComment(user: UserInternal, ticketId: Long, textBody: String) = coroutineScope.launch {
        val command = localCommandsStore.addTextCommand(user, ticketId, textBody)
        syncCommand(command)
    }

    fun readTicket(user: UserInternal, ticketId: Long) = coroutineScope.launch {
        val command = localCommandsStore.addReadCommand(user, ticketId)
        val syncTry = synchronizer.syncCommand(command)
        if (syncTry.isSuccess()) {
            localCommandsStore.removeCommand(command.commandId)
        }
    }

    fun addAttachComment(user: UserInternal, ticketId: Long, fileUri: Uri) = coroutineScope.launch {

        val fileData = fileResolver.getFileData(fileUri) ?: return@launch
        val commandEntity = localCommandsStore.addAttachmentCommand(user, ticketId, fileData)
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

        val command = SyncRequest.Command.CreateComment(
            localId = commandEntity.localId,
            commandId = commandEntity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = commandEntity.creationTime,
            requestNewTicket = false, // TODO to upper level
            ticketId = ticketId,
            comment = null,
            attachments = listOf(repositoryMapper.map(newLocalAttachment)),
            rating = null,
        )

        syncCommand(command)
    }

    fun addRatingComment(user: UserInternal, ticketId: Long, rating: Int) = coroutineScope.launch {
        val command = localCommandsStore.addRatingCommand(user, ticketId, rating)
        syncCommand(command)
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

    fun removeCommandError(commandId: String) {
        localCommandsStore.removeCommandError(commandId)
    }

    private suspend fun syncCommand(command: SyncRequest.Command) {
        val syncTry = synchronizer.syncCommand(command)

        if (syncTry.isSuccess()) {
            localCommandsStore.removeCommand(command.commandId)
        }
        else {
            val errorEntity = repositoryMapper.mapToCommandErrorEntity(command)
            localCommandsStore.addCommandError(errorEntity)
        }
    }

    private fun mergeComments(local: List<Comment>, remote: FullTicket): FullTicket {
        val comments = ArrayList<Comment>(local)
        comments.addAll(remote.comments)
        comments.sortBy { it.creationTime }
        return remote.copy(comments = comments)
    }

}