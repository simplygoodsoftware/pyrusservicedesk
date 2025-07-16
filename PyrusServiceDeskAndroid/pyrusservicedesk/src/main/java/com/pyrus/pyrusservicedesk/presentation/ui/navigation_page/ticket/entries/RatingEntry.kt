package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.RatingSettings

internal data class RatingEntry(
    val ratingSettings: RatingSettings?,
    val ratingText: String?,
)