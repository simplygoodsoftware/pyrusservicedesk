package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Short description of the ticket that can be used for listing available tickets and for counting
 * tickets that are currently unread.
 *
 * @param isRead FALSE is there are pending comments from support that are not seen yet.
 * @param lastComment last comment of the ticket.
 */

@JsonClass(generateAdapter = true)
internal class TicketShortDescriptionDto(
    @Json(name = "ticket_id") val ticketId: Int,
    @Json(name = "subject") val subject: String,
    @Json(name = "is_read") val isRead: Boolean = true,
    @Json(name = "last_comment") val lastComment: CommentDto?,
)