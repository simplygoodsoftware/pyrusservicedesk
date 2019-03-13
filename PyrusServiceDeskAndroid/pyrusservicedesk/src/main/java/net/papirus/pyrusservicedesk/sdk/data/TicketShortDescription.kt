package net.papirus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal class TicketShortDescription(
    @SerializedName("ticket_id")
    val ticketId: Int,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("is_read")
    val isRead: Boolean,
    @SerializedName("last_comment")
    val lastComment: Comment?)