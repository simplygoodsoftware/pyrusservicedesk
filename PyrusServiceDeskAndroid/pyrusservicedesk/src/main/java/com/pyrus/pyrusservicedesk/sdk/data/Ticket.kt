package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import java.util.Date

/**
 * Default value for [Ticket.ticketId]
 */
internal const val EMPTY_TICKET_ID = 0

/**
 * Represents ticket object.
 */
internal data class Ticket(
    @SerializedName("ticket_id") val ticketId: Int = EMPTY_TICKET_ID,
    @SerializedName("user_id") val userId: String? = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("author") val author: String? = "",
    @SerializedName("is_read") val isRead: Boolean?,
    @SerializedName("last_comment") val lastComment: CommentDto?,
    @SerializedName("comments") val comments: List<CommentDto>? = null,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("created_at") val createdAt: Date?, //TODO check it
    @SerializedName("show_rating") val showRating: Boolean = false,
    @SerializedName("show_rating_text") val showRatingText: String? = "",
)