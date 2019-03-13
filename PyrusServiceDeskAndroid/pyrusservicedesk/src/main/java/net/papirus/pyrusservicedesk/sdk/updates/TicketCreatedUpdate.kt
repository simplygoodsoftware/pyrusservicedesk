package net.papirus.pyrusservicedesk.sdk.updates

internal class TicketCreatedUpdate(
        val ticketId: Int? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type = UpdateType.TicketCreated
}