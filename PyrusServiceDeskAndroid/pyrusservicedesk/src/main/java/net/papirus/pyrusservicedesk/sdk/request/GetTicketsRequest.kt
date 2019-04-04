package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

/**
 * Request for obtaining list of [TicketShortDescription]
 */
internal class GetTicketsRequest(repository: GeneralRepository): RequestBase<List<TicketShortDescription>>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseBase<List<TicketShortDescription>> = repository.getTickets()
}
