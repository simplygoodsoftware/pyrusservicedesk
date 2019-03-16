package net.papirus.pyrusservicedesk.sdk

import net.papirus.pyrusservicedesk.sdk.request.AddCommentRequest1
import net.papirus.pyrusservicedesk.sdk.request.CreateTicketRequest1
import net.papirus.pyrusservicedesk.sdk.response.*

internal class CentralRepository(private val webRepository: Repository) : Repository {

    override fun getConversation(): GetConversationResponse1 = webRepository.getConversation()

    override fun getTickets(): GetTicketsResponse1 = webRepository.getTickets()

    override fun getTicket(ticketId: Int): GetTicketResponse1 = webRepository.getTicket(ticketId)

    override fun addComment(request: AddCommentRequest1): AddCommentResponse1 = webRepository.addComment(request)

    override fun createTicket(request: CreateTicketRequest1): CreateTicketResponse1 = webRepository.createTicket(request)
}