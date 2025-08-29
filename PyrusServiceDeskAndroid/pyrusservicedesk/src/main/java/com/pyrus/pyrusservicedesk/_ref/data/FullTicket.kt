package com.pyrus.pyrusservicedesk._ref.data

data class FullTicket(
    val subject: String?,
    val comments: List<Comment>,
    val showRating: Boolean,
    val showRatingText: String?,
    val ratingSettings: RatingSettings?,
    val orgLogoUrl: String?,
    val userId: String,
    val ticketId: Long,
    val isActive: Boolean,
    val isRead: Boolean,
)