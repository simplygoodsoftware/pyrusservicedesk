package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID

internal class CommentAddedUpdate(
        val ticketId: Int = EMPTY_TICKET_ID,
        val comment: Comment,
        val isNew: Boolean = false,
        error: UpdateError? = null)
    : UpdateBase(error) {

    override val type = UpdateType.TicketUpdated
}