package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import com.pyrus.pyrusservicedesk.utils.Try
import com.pyrus.pyrusservicedesk.utils.isSuccess

/**
 * Created by smokedealer on 26.01.2024.
 */

internal class Repository(
    private val localStore: LocalStore,
    private val remoteStore: RemoteStore,
) {

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean, requestsRemoteComments: Boolean) = when {
        requestsRemoteComments -> remoteStore.getFeed(keepUnread)
        else -> localStore.getPendingFeedComments()
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

    suspend fun removePendingComment(comment: Comment): Try<Boolean> {
        return localStore.removePendingComment(comment)
    }


}