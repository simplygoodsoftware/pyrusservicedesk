package com.pyrus.pyrusservicedesk.sdk

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CreateTicketResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.request.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class RequestFactory(private val repository: GeneralRepository) {

    fun getFeedRequest(): Request<Comments> = GetFeedRequest(repository, true)
    fun getTicketsRequest(): RequestBase<List<TicketShortDescription>> = GetTicketsRequest(repository)
    fun getTicketRequest(ticketId: Int): RequestBase<Ticket> = GetTicketRequest(repository, ticketId)
    fun getCreateTicketRequest(description: TicketDescription,
                               uploadFileHooks: UploadFileHooks?): RequestBase<CreateTicketResponseData> {
        return CreateTicketRequest(repository, description, uploadFileHooks)
    }

    fun getAddFeedCommentRequest(comment: Comment,
                                 uploadFileHooks: UploadFileHooks? = null): RequestBase<AddCommentResponseData> {
        return AddFeedCommentRequest(repository, comment, uploadFileHooks)
    }

    fun getAddCommentRequest(ticketId: Int,
                             comment: Comment,
                             uploadFileHooks: UploadFileHooks? = null): RequestBase<AddCommentResponseData> {
        return AddCommentRequest(repository, ticketId, comment, uploadFileHooks)
    }

    /**
     * @param token can be null. Push Notifications will stop then.
     */
    fun getSetPushTokenRequest(token: String?): RequestBase<Unit> {
        return SetPushTokenRequest(repository, token)
    }

    fun getPendingFeedCommentsRequest(): Request<Comments> = GetFeedRequest(repository, false)
    fun getRemovePendingCommentRequest(comment: Comment) = RemovePendingCommentRequest(repository, comment)
    fun getRemoveAllPendingCommentsRequest() = RemoveAllPendingCommentsRequest(repository)
}