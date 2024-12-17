package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk.sdk.data.Author
import com.pyrus.pyrusservicedesk.sdk.data.COMMENT_ID_EMPTY
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong


internal class Repository(
    private val localStore: LocalStore,
    private val remoteStore: RemoteStore,
) {

    private val lastLocalCommentId = AtomicLong(localStore.getPendingFeedComments().lastOrNull()?.localId ?: 0L)

    private val remoteFeedStateFlow: MutableStateFlow<Comments?> = MutableStateFlow(null)
    private val remoteFeedMutex = Mutex()

    fun getFeedFlow(): Flow<Comments?> = combine(localStore.commentsFlow(), remoteFeedStateFlow) { local, remote ->
        when (remote) {
            null -> Comments(local)
            else -> mergeComments(local, remote)
        }
    }

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean, includePendingComments: Boolean = false): Try<Comments> {
        val feedTry = remoteStore.getFeed(keepUnread)
        if (feedTry.isSuccess()) remoteFeedMutex.withLock {
            remoteFeedStateFlow.value = feedTry.value
        }

        if (includePendingComments) {
            return feedTry.map { mergeComments(localStore.getPendingFeedComments(), it) }
        }
        return feedTry
    }

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(): Try<List<TicketShortDescription>> {
        return remoteStore.getTickets()
    }

    suspend fun addTextComment(textBody: String) {
        val comment = createLocalTextComment(textBody).copy(isSending = true)
        localStore.addPendingFeedComment(comment)
        val response = remoteStore.addTextComment(textBody)

        if (response.isSuccess()) {
            localStore.removePendingComment(comment)
            addNewCommentToState(response.value.commentId, comment)
        }
        else {
            localStore.addPendingFeedComment(comment.copy(isSending = false))
        }
    }

    suspend fun addAttachComment() {
//        val comment = createLocalAttachComment().compy(isSending = true)
//        localStore.addPendingFeedComment(comment)
//        val response = remoteStore.addAttachComment()
//
//
//        if (response.isSuccess()) {
//            localStore.removePendingComment(comment)
//            addNewCommentToState(response.value.commentId, comment)
//        }
//        else {
//            localStore.addPendingFeedComment(comment.copy(isSending = false))
//        }
    }

    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addFeedComment(
        comment: CommentDto,
        uploadFileHooks: UploadFileHooks?
    ): Try<AddCommentResponseData> {
        localStore.addPendingFeedComment(comment)
        val response = remoteStore.addFeedComment(comment, uploadFileHooks)
        if (response.isSuccess()) {
            localStore.removePendingComment(comment)
        }
        return response
    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(token: String?, tokenType: String): Try<Unit> {
        return remoteStore.setPushToken(token, tokenType)
    }

    fun removePendingComment(comment: CommentDto) {
        return localStore.removePendingComment(comment)
    }

    private suspend fun addNewCommentToState(remoteId: Long, localComment: CommentDto) {
        remoteFeedMutex.withLock {
            val remoteComment = localComment.copy(commentId = remoteId, localId = COMMENT_ID_EMPTY, isSending = false)
            val comments = remoteFeedStateFlow.value ?: Comments()
            remoteFeedStateFlow.value = comments.copy(comments = comments.comments + remoteComment)
        }
    }

    private fun mergeComments(local: List<CommentDto>, remote: Comments): Comments {
        val comments = ArrayList<CommentDto>(local)
        comments.addAll(remote.comments)
        comments.sortBy { it.creationDate.time }
        return remote.copy(comments = comments)
    }

    private fun createLocalCommentId(): Long = lastLocalCommentId.getAndDecrement()

    private fun createLocalTextComment(text: String) = CommentDto(
        body = text,
        isInbound = true,
        author = Author(ConfigUtils.getUserName()),
        attachments = null,
        creationDate = Calendar.getInstance().time, // TODO
        localId = createLocalCommentId(),
        rating = null
    )

    private fun createLocalAttachComment(): CommentDto {
        return CommentDto(
            body = null,
            isInbound = true,
            attachments = TODO(),
            creationDate = Calendar.getInstance().time, // TODO
            author = Author(ConfigUtils.getUserName()),
            localId = createLocalCommentId(),
            rating = null,
        )

    }

}