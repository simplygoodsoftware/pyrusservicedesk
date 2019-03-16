package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus
import net.papirus.pyrusservicedesk.sdk.data.Ticket

internal class GetTicketResponse1(
    status: ResponseStatus,
    ticket: Ticket? = null)
    : ResponseBase1<Ticket>(status, ticket)
