package net.papirus.pyrusservicedesk.repository.updates

import net.papirus.pyrusservicedesk.repository.data.Comment
import net.papirus.pyrusservicedesk.repository.data.EMPTY_TICKET_ID

internal class CommentAddedUpdate(
        val ticketId: Int = EMPTY_TICKET_ID,
        val comment: Comment? = null,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type = UpdateType.TicketUpdated
}