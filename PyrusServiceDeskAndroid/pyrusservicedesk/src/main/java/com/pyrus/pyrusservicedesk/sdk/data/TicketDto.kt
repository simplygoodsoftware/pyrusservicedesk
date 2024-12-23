package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Default value for [Ticket.ticketId]
 */

/**
 * Represents ticket object.
 */
internal data class TicketDto(
    @SerializedName("ticket_id") val ticketId: Int,
    @SerializedName("user_id") val userId: String? = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("author") val author: String? = "",
    @SerializedName("is_read") val isRead: Boolean?,
    @SerializedName("last_comment") val lastComment: CommentDto?,
    @SerializedName("comments") val comments: List<CommentDto>? = null,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("created_at") val createdAt: Date?, //TODO check it
    @SerializedName("show_rating") val showRating: Boolean?,
    @SerializedName("show_rating_text") val showRatingText: String? = "",
)