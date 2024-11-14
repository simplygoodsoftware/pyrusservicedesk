package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription

/**
 * Response on [GetTicketsRequest]
 */
internal class GetTicketsResponse(
    error: ResponseError? = null,
    tickets: List<Ticket>? = null)
    : ResponseImpl<List<Ticket>>(error, tickets)
