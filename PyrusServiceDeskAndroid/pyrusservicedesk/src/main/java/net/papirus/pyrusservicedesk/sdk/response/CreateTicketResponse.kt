package net.papirus.pyrusservicedesk.sdk.response

internal class CreateTicketResponse(
    error: ResponseError? = null,
    ticketId: Int? = null)
    : ResponseBase<Int>(error, ticketId)
