package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request

import net.papirus.pyrusservicedesk.repository.data.TicketDescription
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.CreateTicketRequestBody

internal class CreateTicketRequest(
        val userName: String,
        val ticket: TicketDescription
)
    : RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): CreateTicketRequestBody {
        return CreateTicketRequestBody(appId, userId, userName, ticket)
    }

}
