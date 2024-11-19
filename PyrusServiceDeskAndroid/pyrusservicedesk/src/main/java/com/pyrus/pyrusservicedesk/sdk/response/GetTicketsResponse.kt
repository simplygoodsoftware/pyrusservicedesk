package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets

/**
 * Response on [GetTicketsRequest]
 */
internal class GetTicketsResponse(
    error: ResponseError? = null,
    tickets: Tickets? = null)
    : ResponseImpl<Tickets>(error, tickets)
