package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.Ticket

internal class GetTicketUpdate(
        val ticket: Ticket? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.TicketReceived
}
