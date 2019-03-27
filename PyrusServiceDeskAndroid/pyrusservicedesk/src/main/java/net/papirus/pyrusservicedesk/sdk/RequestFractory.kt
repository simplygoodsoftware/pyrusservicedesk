package net.papirus.pyrusservicedesk.sdk

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.request.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

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
}