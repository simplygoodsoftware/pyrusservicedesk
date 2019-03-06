package net.papirus.pyrusservicedesk.repository.updates

import net.papirus.pyrusservicedesk.repository.data.Ticket

internal class GetTicketsUpdate(
        val tickets: List<Ticket>? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.TicketsReceived

}
