package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request

import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body.RequestBodyBase

internal class GetTicketRequest(val ticketId: Int): RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): RequestBodyBase {
        return RequestBodyBase(appId, userId)
    }
}
