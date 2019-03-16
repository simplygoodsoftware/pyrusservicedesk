package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription

internal class GetTicketsResponse1(
    status: ResponseStatus,
    tickets: List<TicketShortDescription>? = null)
    : ResponseBase1<List<TicketShortDescription>>(status, tickets)
