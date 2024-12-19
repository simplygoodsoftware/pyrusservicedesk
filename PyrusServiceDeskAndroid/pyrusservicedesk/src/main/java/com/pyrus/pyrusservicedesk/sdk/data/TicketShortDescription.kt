package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Short description of the ticket that can be used for listing available tickets and for counting
 * tickets that are currently unread.
 *
 * @param isRead FALSE is there are pending comments from support that are not seen yet.
 * @param lastComment last comment of the ticket.
 */
internal class TicketShortDescription(
    @SerializedName("ticket_id") val ticketId: Int,
    @SerializedName("subject") val subject: String,
    @SerializedName("is_read") val isRead: Boolean = true,
    @SerializedName("last_comment") val lastComment: CommentDto?,
)