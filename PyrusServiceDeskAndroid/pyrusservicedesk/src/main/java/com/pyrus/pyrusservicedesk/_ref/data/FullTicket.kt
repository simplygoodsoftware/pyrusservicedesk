package com.pyrus.pyrusservicedesk._ref.data

internal data class FullTicket(
    val subject: String?,
    val comments: List<Comment>,
    val showRating: Boolean,
    val showRatingText: String?,
    val orgLogoUrl: String?,
    val userId: String,
    val ticketId: Long,
)