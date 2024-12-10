package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

/**
 * Created by smokedealer on 26.01.2024.
 */

internal class Repository(
    private val localStore: LocalStore,
    private val remoteStore: RemoteStore,
) {

    private val remoteFeedStateFlow: MutableStateFlow<Comments?> = MutableStateFlow(null)

    fun getFeedFlow() = combine(localStore.commentsFlow(), remoteFeedStateFlow) { local, remote ->
        when (remote) {
            null -> Comments(local)
            else -> {
                val comments = ArrayList<Comment>(local)
                comments.addAll(remote.comments)
                comments.sortBy { it.creationDate.time }
                remote.copy(comments = comments)
            }
        }
    }

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean): Try<Comments> {
        val feedTry = remoteStore.getFeed(keepUnread)
        if (feedTry.isSuccess()) remoteFeedStateFlow.value = feedTry.value
        return feedTry
    }

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(): Try<List<TicketShortDescription>> {
        return remoteStore.getTickets()
    }

    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addFeedComment(
        comment: Comment,
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

    fun removePendingComment(comment: Comment) {
        return localStore.removePendingComment(comment)
    }


}