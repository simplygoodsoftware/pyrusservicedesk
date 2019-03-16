package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus

internal class CreateTicketResponse1(
    status: ResponseStatus,
    ticketId: Int? = null)
    : ResponseBase1<Int>(status, ticketId)
