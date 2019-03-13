package net.papirus.pyrusservicedesk.sdk.web_service.response

import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.RequestBase

internal class GetTicketsResponse(
        status: Status = Status.Ok,
        request: RequestBase,
        tickets: List<TicketShortDescription>? = null)
    : ResponseBase<RequestBase, List<TicketShortDescription>>(status, request, tickets)
