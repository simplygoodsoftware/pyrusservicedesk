package net.papirus.pyrusservicedesk.sdk

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CentralRepository(private val webRepository: Repository) : Repository {

    override fun getConversation(): GetConversationResponse = webRepository.getConversation()

    override fun getTickets(): GetTicketsResponse = webRepository.getTickets()

    override fun getTicket(ticketId: Int): GetTicketResponse = webRepository.getTicket(ticketId)

    override fun addComment(ticketId: Int,
                            comment: Comment,
                            uploadFileHooks: UploadFileHooks)
            : AddCommentResponse = webRepository.addComment(ticketId, comment, uploadFileHooks)

    override fun createTicket(
        description: TicketDescription,
        uploadFileHooks: UploadFileHooks
    )
            : CreateTicketResponse = webRepository.createTicket(description, uploadFileHooks)
}