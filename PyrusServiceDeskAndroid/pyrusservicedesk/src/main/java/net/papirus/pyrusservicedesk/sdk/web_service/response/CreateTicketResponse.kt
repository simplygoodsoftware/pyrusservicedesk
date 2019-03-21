package net.papirus.pyrusservicedesk.sdk.web_service.response

import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.CreateTicketRequest

internal class CreateTicketResponse(
        status: Status = Status.Ok,
        request: CreateTicketRequest,
        ticketId: Int? = null)
    : ResponseBase<CreateTicketRequest, Int>(status, request, ticketId)