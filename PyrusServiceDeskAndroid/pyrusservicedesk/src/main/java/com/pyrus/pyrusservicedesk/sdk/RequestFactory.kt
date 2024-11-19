package com.pyrus.pyrusservicedesk.sdk

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.request.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class RequestFactory(private val repository: GeneralRepository) {

    fun getFeedRequest(keepUnread: Boolean = false): Request<Comments> = GetFeedRequest(repository, true, keepUnread)
    fun getTicketsRequest(): RequestBase<Tickets> = GetTicketsRequest(repository)

    fun getTicketRequest(ticketId: Int): RequestBase<Ticket> = GetTicketRequest(repository, ticketId)

    fun getAddFeedCommentRequest(
        ticketId: Int,
        comment: Comment,
        uploadFileHooks: UploadFileHooks? = null
    ): RequestBase<AddCommentResponseData> {
        return AddFeedCommentRequest(repository, ticketId, comment, uploadFileHooks)
    }

    /**
     * @param token can be null. Push Notifications will stop then.
     * @param tokenType cloud messaging type.
     */
    fun getSetPushTokenRequest(token: String?, tokenType: String): RequestBase<Unit> {
        return SetPushTokenRequest(repository, token, tokenType)
    }

    fun getPendingFeedCommentsRequest(): Request<Comments> = GetFeedRequest(repository, false, false)
    fun getRemovePendingCommentRequest(comment: Comment) = RemovePendingCommentRequest(repository, comment)
    fun getRemoveAllPendingCommentsRequest() = RemoveAllPendingCommentsRequest(repository)
}