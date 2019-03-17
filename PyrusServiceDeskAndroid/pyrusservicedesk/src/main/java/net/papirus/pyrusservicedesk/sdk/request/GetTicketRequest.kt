package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

internal class GetTicketRequest(repository: Repository, val ticketId: Int): RequestBase<Ticket>(repository) {
    override fun run(repository: Repository): ResponseBase<Ticket> = repository.getTicket(ticketId)
}
