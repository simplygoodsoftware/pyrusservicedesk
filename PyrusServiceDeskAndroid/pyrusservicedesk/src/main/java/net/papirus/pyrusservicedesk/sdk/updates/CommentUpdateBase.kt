package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID

internal abstract class CommentUpdateBase(
        val ticketId: Int = EMPTY_TICKET_ID,
        val comment: Comment,
        error: UpdateError? = null)
    : UpdateBase(error)