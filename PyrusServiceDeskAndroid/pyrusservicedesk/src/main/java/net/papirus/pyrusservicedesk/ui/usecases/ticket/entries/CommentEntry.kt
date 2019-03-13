package net.papirus.pyrusservicedesk.ui.usecases.ticket.entries

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.updates.UpdateError

internal class CommentEntry(
        val comment: Comment,
        val updateError: UpdateError? = null)
    : TicketEntry{

    fun hasError() = updateError != null
}