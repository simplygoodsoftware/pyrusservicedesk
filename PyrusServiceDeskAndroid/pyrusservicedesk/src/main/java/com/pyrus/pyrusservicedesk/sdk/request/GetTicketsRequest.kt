package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl

/**
 * Request for obtaining list of [TicketShortDescription]
 */
internal class GetTicketsRequest(repository: GeneralRepository): RequestBase<List<TicketShortDescription>>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseImpl<List<TicketShortDescription>> = repository.getTickets()
}
