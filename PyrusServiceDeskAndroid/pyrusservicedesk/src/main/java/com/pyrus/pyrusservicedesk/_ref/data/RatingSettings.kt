package com.pyrus.pyrusservicedesk._ref.data

import androidx.room.ColumnInfo

/**
 * Represents ticket object.
 * @param size size of the rating scale
 * @param type type of the rating scale
 * @param ratingTextValues rating text values
 */

data class RatingSettings(
    @ColumnInfo(name = "size")
    val size: Int?,
    @ColumnInfo(name = "type")
    val type: Int?,
    @ColumnInfo(name = "rating_text_values")
    val ratingTextValues: List<RatingTextValues>?,
)