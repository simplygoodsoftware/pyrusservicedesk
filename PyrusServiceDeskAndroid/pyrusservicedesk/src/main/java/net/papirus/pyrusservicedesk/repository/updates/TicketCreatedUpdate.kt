package net.papirus.pyrusservicedesk.repository.updates

internal class TicketCreatedUpdate(
        val ticketId: Int? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type = UpdateType.TicketCreated
}