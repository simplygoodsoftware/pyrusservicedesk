package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request

import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.CreateTicketRequestBody

internal class CreateTicketRequest(val ticket: Ticket): RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): CreateTicketRequestBody {
        return CreateTicketRequestBody(appId, userId, ticket)
    }

}
