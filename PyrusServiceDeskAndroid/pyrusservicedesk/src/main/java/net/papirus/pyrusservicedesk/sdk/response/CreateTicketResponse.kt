package net.papirus.pyrusservicedesk.sdk.response

/**
 * Response on [CreateTicketRequest]
 */
internal class CreateTicketResponse(
    error: ResponseError? = null,
    ticketId: Int? = null)
    : ResponseBase<Int>(error, ticketId)
