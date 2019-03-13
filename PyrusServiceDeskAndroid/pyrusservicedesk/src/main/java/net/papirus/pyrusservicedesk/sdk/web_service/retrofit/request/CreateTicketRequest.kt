package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request

import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body.CreateTicketRequestBody

internal class CreateTicketRequest(
        val userName: String,
        val ticket: TicketDescription
)
    : RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): CreateTicketRequestBody {
        return CreateTicketRequestBody(appId, userId, userName, ticket)
    }

}
