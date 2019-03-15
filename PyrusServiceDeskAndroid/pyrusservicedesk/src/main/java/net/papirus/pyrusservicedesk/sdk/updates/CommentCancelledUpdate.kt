package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID

internal class CommentCancelledUpdate(
        ticketId: Int = EMPTY_TICKET_ID,
        comment: Comment)
    : CommentUpdateBase(ticketId, comment) {

    override val type = UpdateType.CommentCancelled
}