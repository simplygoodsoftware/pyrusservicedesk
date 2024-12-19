package com.pyrus.pyrusservicedesk._ref.data

internal data class FullTicket(
    val comments: List<Comment>,
    val showRating: Boolean,
    val showRatingText: String?,
)