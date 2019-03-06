package net.papirus.pyrusservicedesk.repository.web_service.response

import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.GetTicketRequest

internal class GetTicketResponse(
        status: Status = Status.Ok,
        request: GetTicketRequest,
        ticket: Ticket? = null)
    : ResponseBase<GetTicketRequest, Ticket>(status, request, ticket)
