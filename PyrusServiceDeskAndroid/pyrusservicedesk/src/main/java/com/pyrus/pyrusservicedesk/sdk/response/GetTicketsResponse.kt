package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription

/**
 * Response on [GetTicketsRequest]
 */
internal class GetTicketsResponse(
    error: ResponseError? = null,
    tickets: List<TicketShortDescription>? = null)
    : ResponseBase<List<TicketShortDescription>>(error, tickets)
