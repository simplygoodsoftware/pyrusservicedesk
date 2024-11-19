package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl

/**
 * Request for obtaining list of [Ticket]
 */
internal class GetTicketsRequest(repository: GeneralRepository): RequestBase<Tickets>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseImpl<Tickets> = repository.getTickets()
}
