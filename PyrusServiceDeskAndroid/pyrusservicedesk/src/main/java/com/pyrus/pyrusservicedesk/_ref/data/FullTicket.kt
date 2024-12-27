package com.pyrus.pyrusservicedesk._ref.data

internal data class FullTicket(
    val subject: String?,
    val isRead: Boolean,
    val lastComment: Comment?,
    val comments: List<Comment>,
    val showRating: Boolean,
    val showRatingText: String?,
    val userId: String?, // TODO почему userId нулабелен
    val ticketId: Int,
)