package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong


internal class Repository(
    private val localStore: LocalStore,
    private val remoteStore: RemoteStore,
    private val repositoryMapper: RepositoryMapper,
) {

    private val lastLocalCommentId = AtomicLong(localStore.getPendingFeedComments().lastOrNull()?.id ?: 0L)

    private val remoteFeedStateFlow: MutableStateFlow<FullTicket?> = MutableStateFlow(null)
    private val remoteFeedMutex = Mutex()

    fun getFeedFlow(): Flow<FullTicket?> = combine(localStore.commentsFlow(), remoteFeedStateFlow) { local, remote ->
        when (remote) {
            null -> FullTicket(local, false, null)
            else -> mergeComments(local, remote)
        }
    }

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean, includePendingComments: Boolean = false): Try<FullTicket> {
        val feedTry = remoteStore.getFeed(keepUnread).map(repositoryMapper::map)
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
        val comment = createLocalTextComment(textBody)
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
//    suspend fun addFeedComment(
//        comment: Comment,
//        uploadFileHooks: UploadFileHooks?
//    ): Try<AddCommentResponseData> {
//        localStore.addPendingFeedComment(comment)
//        val response = remoteStore.addFeedComment(comment, uploadFileHooks)
//        if (response.isSuccess()) {
//            localStore.removePendingComment(comment)
//        }
//        return response
//    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(token: String?, tokenType: String): Try<Unit> {
        return remoteStore.setPushToken(token, tokenType)
    }

    fun removePendingComment(comment: Comment) {
        return localStore.removePendingComment(comment)
    }

    private suspend fun addNewCommentToState(remoteId: Long, localComment: Comment) {
        remoteFeedMutex.withLock {
            val remoteComment = localComment.copy(id = remoteId, isLocal = false, isSending = false)
            val ticket = remoteFeedStateFlow.value ?: FullTicket(emptyList(), false, null)
            remoteFeedStateFlow.value = ticket.copy(comments = ticket.comments + remoteComment)
        }
    }

    private fun mergeComments(local: List<Comment>, remote: FullTicket): FullTicket {
        val comments = ArrayList<Comment>(local)
        comments.addAll(remote.comments)
        comments.sortBy { it.creationTime }
        return remote.copy(comments = comments)
    }

    private fun createLocalCommentId(): Long = lastLocalCommentId.getAndDecrement()

    private fun createLocalTextComment(text: String) = Comment(
        body = text,
        isInbound = true,
        author = Author(ConfigUtils.getUserName(), null, "#fffffff"),
        attachments = null,
        creationTime = System.currentTimeMillis(),
        id = createLocalCommentId(),
        isLocal = true,
        rating = null,
        isSending = true
    )

    private fun createLocalAttachComment(): Comment {
        return Comment(
            id = createLocalCommentId(),
            body = null,
            isInbound = true,
            attachments = null, // TODO
            creationTime = System.currentTimeMillis(),
            author = Author(ConfigUtils.getUserName(), null, "#fffffff"),
            rating = null,
            isLocal = true,
            isSending = true
        )

    }

}