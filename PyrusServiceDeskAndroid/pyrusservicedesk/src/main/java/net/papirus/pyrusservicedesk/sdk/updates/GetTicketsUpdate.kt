package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription

internal class GetTicketsUpdate(
        val tickets: List<TicketShortDescription>? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.TicketsReceived

}
