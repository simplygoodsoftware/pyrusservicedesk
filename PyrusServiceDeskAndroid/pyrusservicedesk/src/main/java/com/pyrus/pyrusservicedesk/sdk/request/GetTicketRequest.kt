package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseBase

/**
 * Request for obtaining ticket.
 */
internal class GetTicketRequest(repository: GeneralRepository, val ticketId: Int): RequestBase<Ticket>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseBase<Ticket> = repository.getTicket(ticketId)
}
