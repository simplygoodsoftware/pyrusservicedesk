package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
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
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.CommandDto
import com.pyrus.pyrusservicedesk.sdk.data.CreateCommentDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max


internal class Repository(
    private val localCommandsStore: LocalCommandsStore,
    private val remoteStore: RemoteStore,
    private val repositoryMapper: RepositoryMapper,
    private val fileResolver: FileResolver,
    private val remoteFileStore: RemoteFileStore,
) {

    private val lastLocalCommentId: AtomicLong
    private val lastLocalAttachmentId: AtomicInteger

    private val remoteFeedStateFlow: MutableStateFlow<FullTicket?> = MutableStateFlow(null)
    private val remoteFeedMutex = Mutex()

    private val fileHooks = ConcurrentHashMap<Int, UploadFileHook>()

    init {
        val pendingComments = localCommandsStore.getPendingFeedComments()
        lastLocalCommentId = AtomicLong(pendingComments.lastOrNull()?.id ?: 0L)

        var lastAttachmentId = 0
        for (comment in pendingComments) {
            for (attach in comment.attachments ?: continue) {
                lastAttachmentId = max(attach.id, lastAttachmentId)
            }
        }
        lastLocalAttachmentId = AtomicInteger(lastAttachmentId)
    }

    // TODO sdssds
    fun getFeedFlow(ticketId: Int): Flow<FullTicket?> = combine(localCommandsStore.commentsFlow(), remoteFeedStateFlow) { local, remote ->
        when (remote) {
            null -> FullTicket(
                comments = local,
                showRating = false,
                showRatingText = null,
                userId = null,
                ticketId = ticketId,
                subject = null,
                isRead = true,
                lastComment = local.lastOrNull(),
            )
            else -> mergeComments(local, remote)
        }
    }

    /**
     * Provides tickets in single feed representation.
     */
//    suspend fun getFeed(keepUnread: Boolean, includePendingComments: Boolean = false): Try<FullTicket> {
//        val feedTry = remoteStore.getFeed(keepUnread).map(repositoryMapper::map)
//        if (feedTry.isSuccess()) remoteFeedMutex.withLock {
//            remoteFeedStateFlow.value = feedTry.value
//        }
//
//        if (includePendingComments) {
//            return feedTry.map { mergeComments(localStore.getPendingFeedComments(), it) }
//        }
//        return feedTry
//    }

    //TODO what we need to do when we haven't fount ticket (feedTry.value == null, but feedTry.isSuccess())
    suspend fun getFeed(
        ticketId: Int,
        keepUnread: Boolean,
        includePendingComments: Boolean = false,
    ): Try<FullTicket> {
        val feedTry = remoteStore.getTicket(ticketId).map {
            repositoryMapper.map(it)
        }

        if (feedTry.isSuccess()) remoteFeedMutex.withLock {
            remoteFeedStateFlow.value = feedTry.value
        }

        if (includePendingComments) {
            return feedTry.map { mergeComments(localCommandsStore.getPendingFeedComments(), it) }
        }
        return feedTry
    }

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(): Try<List<TicketDto>> {
        return remoteStore.getTickets()
    }

    suspend fun getAllData(): Try<TicketsInfo> {
        return remoteStore.getAllData()
    }

    suspend fun addTextComment(ticketId: Int, textBody: String, command: CommandDto) {
        val comment = createLocalTextComment(
            textBody,
            command.commandId
                .substringAfter("commentId=")
                .substringBefore(";")
                .toLong(),
            // TODO wtf
            injector().usersAccount?.authorId
        )

        localCommandsStore.addPendingFeedComment(comment)
        localCommandsStore.addPendingFeedCommand(command)

        val response = remoteStore.addTextComment(TODO())

        if (response.isSuccess() && !response.value.isNullOrEmpty()) {
            localCommandsStore.removePendingCommand(command)
            localCommandsStore.removePendingComment(comment)
            val res = response.value.find { it.commandId == command.commandId }
            if (res != null) {
                addNewCommentToState(ticketId, toAddCommentResponseData(res), comment)
            }
        } else {
            localCommandsStore.addPendingFeedComment(comment.copy(isSending = false))
            localCommandsStore.addPendingFeedCommand(command)
        }
    }

    private fun toAddCommentResponseData(ticketCommandResult: TicketCommandResultDto): AddCommentResponseData {
        return AddCommentResponseData(ticketCommandResult.commentId, null, null)
    }

    suspend fun addAttachComment(ticketId: Int, fileUri: Uri) {
        val fileData = fileResolver.getFileData(fileUri) ?: return
        val localAttachment = createLocalAttachment(fileData)
        val comment = createLocalAttachComment(localAttachment)

        localCommandsStore.addPendingFeedComment(comment)

        val file = fileData.uri.toFile()

        val hook = UploadFileHook()
        fileHooks[localAttachment.id] = hook

        val responseTry = remoteFileStore.uploadFile(file, hook) { progress ->
            Log.d("SDS", "progress: $progress")
            val newLocalAttachment = localAttachment.copy(progress = progress)
            val newLocalComment = comment.copy(attachments = listOf(newLocalAttachment))
            localCommandsStore.addPendingFeedComment(newLocalComment)
        }
        Log.d("SDS", "responseTry: $responseTry")

        val newLocalAttachment = when {
            responseTry.isSuccess() -> localAttachment.copy(
                guid = responseTry.value.guid,
                status = Status.Completed,
                progress = null,
            )
            else -> localAttachment.copy(status = Status.Error, progress = null)
        }
        val newLocalComment = comment.copy(attachments = listOf(newLocalAttachment))
        localCommandsStore.addPendingFeedComment(newLocalComment)
        fileHooks.remove(localAttachment.id)

        val guid = newLocalAttachment.guid ?: return

        val attachmentDto = AttachmentDto(
            guid = guid,
            name = newLocalAttachment.name,
            bytesSize = newLocalAttachment.bytesSize,
            isText = newLocalAttachment.isText,
            isVideo = newLocalAttachment.isVideo,
        )

        val response = remoteStore.addAttachComment(attachmentDto)

        if (response.isSuccess()) {
            localCommandsStore.removePendingComment(comment)
            addNewCommentToState(ticketId, response.value, comment)
        }
        else {
            localCommandsStore.addPendingFeedComment(comment.copy(isSending = false))
        }
    }

    suspend fun addRatingComment(ticketId: Int, rating: Int) {
        val comment = createLocalRatingComment(rating)
        localCommandsStore.addPendingFeedComment(comment)
        val response = remoteStore.addRatingComment(rating)

        if (response.isSuccess()) {
            localCommandsStore.removePendingComment(comment)
            addNewCommentToState(ticketId, response.value, comment)
        }
        else {
            localCommandsStore.addPendingFeedComment(comment.copy(isSending = false))
        }
    }

    suspend fun retryAddComment(ticketId: Int, localId: Long) {
        val command = localCommandsStore.getCommand(localId) ?: return
        val text = (command.params as? CreateCommentDto)?.comment
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
        return remoteStore.setPushToken(token, tokenType)
    }

    fun removePendingComment(comment: Comment) {
        return localCommandsStore.removePendingComment(comment)
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
            val ticket = remoteFeedStateFlow.value ?: FullTicket(
                comments = emptyList(),
                showRating = false,
                showRatingText = null,
                userId = null,
                ticketId = ticketId,
                subject = null,
                isRead = true,
                lastComment = null,
            )
            remoteFeedStateFlow.value = ticket.copy(comments = ticket.comments + remoteComment)
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

    private fun createLocalTextComment(text: String, localId: Long, authorId: String?) = Comment(
        body = text,
        isInbound = true,
        author = Author(ConfigUtils.getUserName(), authorId, null, null),
        attachments = null,
        creationTime = System.currentTimeMillis(),
        id = localId,
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