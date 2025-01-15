package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

/**
 * Default value for [Ticket.ticketId]
 */

/**
 * Represents ticket object.
 */

@JsonClass(generateAdapter = true)
internal data class TicketDto(
    @Json(name = "ticket_id") val ticketId: Long,
    @Json(name = "user_id") val userId: String?,
    @Json(name = "subject") val subject: String,
    @Json(name = "author") val author: String?,
    @Json(name = "is_read") val isRead: Boolean?,
    @Json(name = "last_comment") val lastComment: CommentDto?,
    @Json(name = "comments") val comments: List<CommentDto>?,
    @Json(name = "is_active") val isActive: Boolean?,
    @Json(name = "created_at") val createdAt: Date?, //TODO check it
    @Json(name = "show_rating") val showRating: Boolean?,
    @Json(name = "show_rating_text") val showRatingText: String?,
)