package com.pyrus.pyrusservicedesk.sdk.data

import com.pyrus.pyrusservicedesk.sdk.data.json.DateJ
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Default value for [Ticket.ticketId]
 */

/**
 * Represents ticket object.
 * @param ticketId ticket id.
 * @param userId user id (restaurants id).
 * @param subject ticket title.
 * @param author information about author.
 * @param isRead flag indicating whether the ticket has been read.
 * @param lastComment last comment.
 * @param createdAt ticket creation date.
 * @param showRating flag indicating whether the rating should be shown.
 * @param showRatingText rating text if need.
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
    @Json(name = "created_at") @DateJ val createdAt: Long?,
    @Json(name = "show_rating") val showRating: Boolean?,
    @Json(name = "show_rating_text") val showRatingText: String?,
)