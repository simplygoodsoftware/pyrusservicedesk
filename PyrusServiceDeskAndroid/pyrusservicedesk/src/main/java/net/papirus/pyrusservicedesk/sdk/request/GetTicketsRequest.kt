package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

internal class GetTicketsRequest(repository: Repository): RequestBase<List<TicketShortDescription>>(repository) {
    override suspend fun run(repository: Repository): ResponseBase<List<TicketShortDescription>> = repository.getTickets()
}
