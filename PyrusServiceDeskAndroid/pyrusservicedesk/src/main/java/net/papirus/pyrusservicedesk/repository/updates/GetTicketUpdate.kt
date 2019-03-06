package net.papirus.pyrusservicedesk.repository.updates

import net.papirus.pyrusservicedesk.repository.data.Ticket

internal class GetTicketUpdate(
        val ticket: Ticket? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.TicketReceived
}
