package com.pyrus.pyrusservicedesk.sdk

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.request.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class RequestFactory(private val repository: GeneralRepository) {

    fun getFeedRequest(): RequestBase<List<Comment>> = GetFeedRequest(repository)
    fun getTicketsRequest(): RequestBase<List<TicketShortDescription>> = GetTicketsRequest(repository)
    fun getTicketRequest(ticketId: Int): RequestBase<Ticket> = GetTicketRequest(repository, ticketId)
    fun getCreateTicketRequest(description: TicketDescription,
                               uploadFileHooks: UploadFileHooks?): RequestBase<Int> {
        return CreateTicketRequest(repository, description, uploadFileHooks)
    }

    fun getAddFeedCommentRequest(comment: Comment,
                                 uploadFileHooks: UploadFileHooks? = null): RequestBase<Int> {
        return AddFeedCommentRequest(repository, comment, uploadFileHooks)
    }

    fun getAddCommentRequest(ticketId: Int,
                             comment: Comment,
                             uploadFileHooks: UploadFileHooks? = null): RequestBase<Int> {
        return AddCommentRequest(repository, ticketId, comment, uploadFileHooks)
    }

    fun getSetPushTokenRequest(token: String): RequestBase<Unit> {
        return SetPushTokenRequest(repository, token)
    }
}