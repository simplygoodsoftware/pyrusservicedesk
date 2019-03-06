package net.papirus.pyrusservicedesk.repository.web_service.response

import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.GetTicketsRequest
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.RequestBase

internal class GetTicketsResponse(
        status: Status = Status.Ok,
        request: GetTicketsRequest,
        tickets: List<Ticket>? = null)
    : ResponseBase<RequestBase, List<Ticket>>(status, request, tickets)
