package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

/**
 * Request for obtaining ticket.
 */
internal class GetTicketRequest(repository: GeneralRepository, val ticketId: Int): RequestBase<Ticket>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseBase<Ticket> = repository.getTicket(ticketId)
}
