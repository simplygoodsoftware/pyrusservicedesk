package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.Ticket

/**
 * Response on [GetTicketRequest]
 */
internal class GetTicketResponse(
    error: ResponseError? = null,
    ticket: Ticket? = null)
    : ResponseBase<Ticket>(error, ticket)
