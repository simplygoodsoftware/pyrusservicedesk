package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl

/**
 * Request for obtaining list of [Ticket]
 */
internal class GetTicketsRequest(repository: GeneralRepository): RequestBase<List<Ticket>>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseImpl<List<Ticket>> = repository.getTickets()
}
