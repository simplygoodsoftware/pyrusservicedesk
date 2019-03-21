package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.data.Ticket

internal class GetTicketResponse(
    error: ResponseError? = null,
    ticket: Ticket? = null)
    : ResponseBase<Ticket>(error, ticket)
