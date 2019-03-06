package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body

import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.RequestBodyBase

internal class CreateTicketRequestBody(
        appId: String,
        userId: String,
        val Ticket: Ticket)
    : RequestBodyBase(appId, userId)
