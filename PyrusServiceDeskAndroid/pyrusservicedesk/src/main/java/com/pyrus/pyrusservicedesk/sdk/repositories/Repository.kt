package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


internal class Repository(
    private val localCommandsStore: LocalCommandsStore,
    private val repositoryMapper: RepositoryMapper,
    private val fileResolver: FileResolver,
    private val remoteFileStore: RemoteFileStore,
    private val synchronizer: Synchronizer,
    private val localTicketsStore: LocalTicketsStore,
) {

    private val lastLocalCommentId: AtomicLong
    private val lastLocalAttachmentId: AtomicInteger

    private val remoteFeedMutex = Mutex()

    private val fileHooks = ConcurrentHashMap<Int, UploadFileHook>()

    init {
        // TODO sds
//        val pendingComments = localCommandsStore.getPendingFeedCommands()
//        lastLocalCommentId = AtomicLong(pendingComments.lastOrNull()?.id ?: 0L)
//
//        var lastAttachmentId = 0
//        for (comment in pendingComments) {
//            for (attach in comment.attachments ?: continue) {
//                lastAttachmentId = max(attach.id, lastAttachmentId)
//            }
//        }
//        lastLocalAttachmentId = AtomicInteger(lastAttachmentId)

        lastLocalCommentId = AtomicLong(-1)
        lastLocalAttachmentId = AtomicInteger(-1)
    }

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

    // TODO merge with sync command
    fun getAllDataFlow(): Flow<TicketsInfo?> = localTicketsStore.getTicketInfoFlow().map { dto ->
        dto?.let(repositoryMapper::map)
    }

    //TODO what we need to do when we haven't fount ticket (feedTry.value == null, but feedTry.isSuccess()) (k) sm
    suspend fun getFeed(
        ticketId: Int,
        userId: String,
        force: Boolean,
    ): Try<FullTicket> {

        if (!force) {
            val localTickets: TicketsDto? = localTicketsStore.getTickets()
            val ticket: TicketDto? = localTickets?.tickets?.find { it.ticketId == ticketId }
            if (ticket != null) {
                return Try.Success(repositoryMapper.map(ticket))
            }
        }

        val syncTry = synchronizer.syncData(SyncRequest.Data)
        if (!syncTry.isSuccess()) {
            return syncTry
        }

        val tickets: TicketsDto = syncTry.value
        val ticket: TicketDto? = tickets.tickets?.find { it.ticketId == ticketId }
        if (ticketId < 0 && ticket == null) {
            return Try.Success(repositoryMapper.mapEmptyFullTicket(ticketId, userId))
        }
        if (ticket == null) {
            // TODO в этом случае нужно показывать пользователю ошибку сервера, а не то что у него нет интернета
            // этого можно достич заменив Try на Try2 с кастомными ошибками
            return Try.Failure(Exception("data not found"))
        }

        return Try.Success(repositoryMapper.map(ticket))
    }

    // TODO merge with sync command
    fun getFeedFlow(ticketId: Int): Flow<FullTicket?> = localTicketsStore.getTicketInfoFlow().map { ticketsDto ->
        ticketsDto?.tickets
            ?.find { ticketDto -> ticketDto.ticketId == ticketId }
            ?.let(repositoryMapper::map)
    }









    suspend fun addTextComment(ticketId: Int, textBody: String, command: TicketCommandDto) {
        val comment = createLocalTextComment(
            textBody,
            // TODO wtf
            injector().usersAccount?.authorId
        )

//        localCommandsStore.addPendingFeedComment(comment)
//        localCommandsStore.addPendingFeedCommand(command)
//
//        val response = remoteStore.addTextComment(command)
//
//        if (response.isSuccess() && !response.value.isNullOrEmpty()) {
//            localCommandsStore.removePendingCommand(command)
//            localCommandsStore.removePendingComment(comment)
//            val res = response.value.find { it.commandId == command.commandId }
//            if (res != null) {
//                //addNewCommentToState(ticketId, toAddCommentResponseData(res), comment)
//            }
//        }
//        else {
//            localCommandsStore.addPendingFeedComment(comment.copy(isSending = false))
//            localCommandsStore.addPendingFeedCommand(command)
//        }
    }

    suspend fun readTicket(
        userId: String,
        appId: String,
        ticketId: Int,
    ) {
        // TODO save command to local repo
        val command =  SyncRequest.Command.MarkTicketAsRead(
            commandId = "sdsd",
            userId = userId,
            appId = appId,
            ticketId = ticketId
        )
        synchronizer.syncCommand(command)
    }

    private fun toAddCommentResponseData(ticketCommandResult: TicketCommandResultDto): AddCommentResponseData {
        return AddCommentResponseData(ticketCommandResult.commentId, null, null)
    }

    suspend fun addAttachComment(ticketId: Int, fileUri: Uri) {
        val fileData = fileResolver.getFileData(fileUri) ?: return
        val localAttachment = createLocalAttachment(fileData)
        val comment = createLocalAttachComment(localAttachment)

//        localCommandsStore.addPendingFeedComment(comment)
//
//        val file = fileData.uri.toFile()
//
//        val hook = UploadFileHook()
//        fileHooks[localAttachment.id] = hook
//
//        val responseTry = remoteFileStore.uploadFile(file, hook) { progress ->
//            Log.d("SDS", "progress: $progress")
//            val newLocalAttachment = localAttachment.copy(progress = progress)
//            val newLocalComment = comment.copy(attachments = listOf(newLocalAttachment))
//            localCommandsStore.addPendingFeedComment(newLocalComment)
//        }
//        Log.d("SDS", "responseTry: $responseTry")
//
//        val newLocalAttachment = when {
//            responseTry.isSuccess() -> localAttachment.copy(
//                guid = responseTry.value.guid,
//                status = Status.Completed,
//                progress = null,
//            )
//            else -> localAttachment.copy(status = Status.Error, progress = null)
//        }
//        val newLocalComment = comment.copy(attachments = listOf(newLocalAttachment))
//        localCommandsStore.addPendingFeedComment(newLocalComment)
//        fileHooks.remove(localAttachment.id)
//
//        val guid = newLocalAttachment.guid ?: return
//
//        val attachmentDto = AttachmentDto(
//            guid = guid,
//            name = newLocalAttachment.name,
//            bytesSize = newLocalAttachment.bytesSize,
//            isText = newLocalAttachment.isText,
//            isVideo = newLocalAttachment.isVideo,
//        )
//
//        val response = remoteStore.addAttachComment(attachmentDto)
//
//        if (response.isSuccess()) {
//            localCommandsStore.removePendingComment(comment)
//            addNewCommentToState(ticketId, response.value, comment)
//        }
//        else {
//            localCommandsStore.addPendingFeedComment(comment.copy(isSending = false))
//        }
    }

    suspend fun addRatingComment(ticketId: Int, rating: Int) {
//        val comment = createLocalRatingComment(rating)
//        localCommandsStore.addPendingFeedComment(comment)
//        val response = remoteStore.addRatingComment(rating)
//
//        if (response.isSuccess()) {
//            localCommandsStore.removePendingComment(comment)
//            addNewCommentToState(ticketId, response.value, comment)
//        }
//        else {
//            localCommandsStore.addPendingFeedComment(comment.copy(isSending = false))
//        }
    }

    suspend fun retryAddComment(ticketId: Int, localId: Long) {
        val command = localCommandsStore.getCommand(localId) ?: return
        val text = (command.params as? CommandParamsDto.CreateComment)?.comment
        when {
            //!localComment.attachments.isNullOrEmpty() -> addAttachComment(localComment.attachments.first().uri)//TODO
            !text.isNullOrBlank() -> addTextComment(ticketId, text, command)
            //localComment.rating != null -> addRatingComment(localComment.rating)//TODO
            else -> localCommandsStore.removePendingCommand(command)
        }
    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(token: String?, tokenType: String): Try<Unit> {
//        return remoteStore.setPushToken(token, tokenType)
        TODO()
    }

    fun removePendingComment(comment: Comment) {
        TODO()
//        return localCommandsStore.removePendingComment(comment)
    }

    private suspend fun addNewCommentToState(
        ticketId: Int,
        response: AddCommentResponseData,
        localComment: Comment,
    ) {

        val newAttachments = ArrayList<Attachment>()
        val attachmentIds = response.attachmentIds ?: emptyList()
        val localAttachments = localComment.attachments ?: emptyList()
        if (attachmentIds.size == localAttachments.size) {
            for (i in localAttachments.indices) {
                newAttachments += localAttachments[i].copy(id = attachmentIds[i], status = Status.Completed)
            }
        }

        remoteFeedMutex.withLock {
            val remoteComment = localComment.copy(
//                id = response.commentId,
                id = TODO(),
                isLocal = false,
                isSending = false,
                attachments = if (newAttachments.isEmpty()) null else newAttachments
            )
//            val ticket = remoteFeedStateFlow.value ?: FullTicket(
//                comments = emptyList(),
//                showRating = false,
//                showRatingText = null,
//                userId = null,
//                ticketId = ticketId,
//                subject = null,
//                isRead = true,
//                lastComment = null,
//            )
//            remoteFeedStateFlow.value = ticket.copy(comments = ticket.comments + remoteComment)
        }
    }

    private fun mergeComments(local: List<Comment>, remote: FullTicket): FullTicket {
        val comments = ArrayList<Comment>(local)
        comments.addAll(remote.comments)
        comments.sortBy { it.creationTime }
        return remote.copy(comments = comments)
    }

    fun createLocalCommentId(): Long = lastLocalCommentId.decrementAndGet()

    private fun createLocalAttachmentId(): Int = lastLocalAttachmentId.decrementAndGet()

    private fun createLocalTextComment(text: String, authorId: String?) = Comment(
        body = text,
        isInbound = true,
        author = Author(ConfigUtils.getUserName(), authorId, null, null),
        attachments = null,
        creationTime = System.currentTimeMillis(),
        id = createLocalCommentId(),
        isLocal = true,
        rating = null,
        isSending = true
    )

    private fun createLocalRatingComment(rating: Int) = Comment(
        body = null,
        isInbound = true,
        author = Author(ConfigUtils.getUserName(), null, null, null),
        attachments = null,
        creationTime = System.currentTimeMillis(),
        id = createLocalCommentId(),
        isLocal = true,
        rating = rating,
        isSending = true
    )

    private fun createLocalAttachComment(attachment: Attachment): Comment = Comment(
        id = createLocalCommentId(),
        body = null,
        isInbound = true,
        attachments = listOf(attachment),
        creationTime = System.currentTimeMillis(),
        author = Author(
            name = ConfigUtils.getUserName(),
            // TODO wtf
            authorId = injector().usersAccount?.authorId,
            avatarUrl = null,
            avatarColor = "#fffffff"
        ),
        rating = null,
        isLocal = true,
        isSending = true
    )

    private fun createLocalAttachment(fileData: FileData): Attachment = Attachment(
        id = createLocalAttachmentId(),
        name = fileData.fileName,
        isImage = fileData.fileName.isImage(),
        isText = false,
        bytesSize = fileData.bytesSize,
        isVideo = false,
        uri = fileData.uri,
        status = Status.Processing,
        progress = null,
        guid = null,
    )

}