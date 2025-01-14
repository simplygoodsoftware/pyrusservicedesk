package com.pyrus.pyrusservicedesk._ref.data

internal data class FullTicket(
    val subject: String?,
    val isRead: Boolean,
    val lastComment: Comment?,
    val comments: List<Comment>,
    val showRating: Boolean,
    val showRatingText: String?,
    val isActive: Boolean?,
    val userId: String,
    val ticketId: Long,
)