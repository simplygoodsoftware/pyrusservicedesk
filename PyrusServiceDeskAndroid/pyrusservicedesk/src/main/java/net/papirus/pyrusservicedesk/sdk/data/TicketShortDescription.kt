package net.papirus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal const val NEW_TICKET_ID = -1

internal class TicketShortDescription(
    @SerializedName("ticket_id")
    val ticketId: Int,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("is_read")
    val isRead: Boolean = true,
    @SerializedName("last_comment")
    val lastComment: Comment?)