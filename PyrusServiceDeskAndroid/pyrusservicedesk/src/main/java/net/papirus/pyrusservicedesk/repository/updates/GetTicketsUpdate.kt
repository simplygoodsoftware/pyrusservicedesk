package net.papirus.pyrusservicedesk.repository.updates

import net.papirus.pyrusservicedesk.repository.data.TicketShortDescription

internal class GetTicketsUpdate(
        val tickets: List<TicketShortDescription>? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.TicketsReceived

}
