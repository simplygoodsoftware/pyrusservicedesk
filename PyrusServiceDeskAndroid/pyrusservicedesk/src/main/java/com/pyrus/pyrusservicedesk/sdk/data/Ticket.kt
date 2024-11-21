package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Default value for [Ticket.ticketId]
 */
internal const val EMPTY_TICKET_ID = 0

/**
 * Represents ticket object.
 */
internal data class Ticket(
        @SerializedName("ticket_id")
        val ticketId: Int = EMPTY_TICKET_ID,
        @SerializedName("user_id")
        val userId: String? = "",
        @SerializedName("subject")
        val subject: String = "",
        @SerializedName("author")
        val author: String? = "",
        @SerializedName("is_read")
        val isRead: Boolean?,
        @SerializedName("last_comment")
        val lastComment: Comment?,
        @SerializedName("comments")
        val comments: List<Comment>? = null,
        @SerializedName("is_active")
        val isActive: Boolean?,
        @SerializedName("created_at")
        val createdAt: Date?,
        @SerializedName("show_rating")
        val showRating: Boolean = false,
)