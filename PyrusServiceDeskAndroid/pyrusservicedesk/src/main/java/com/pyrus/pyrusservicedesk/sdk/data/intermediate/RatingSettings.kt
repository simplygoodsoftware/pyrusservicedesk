package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName

data class RatingSettings (
    @SerializedName("size")
    val size: Int?,

    @SerializedName("type")
    val type: Int?,

    @SerializedName("rating_text_values")
    val ratingTextValues: List<RatingTextValues>?,
)

enum class SatisfactionDisplayType {
    None,
    Emoji,
    Like,
    Text
}