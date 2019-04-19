package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Response on [CreateTicketRequest]
 */
internal class CreateTicketResponse(
    error: ResponseError? = null,
    ticketId: Int? = null)
    : ResponseBase<Int>(error, ticketId)