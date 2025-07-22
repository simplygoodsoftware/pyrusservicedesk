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

enum class SatisfactionDisplayType(val  value: Int) {
    None(0),
    Emoji(1),
    Like(2),
    Text(3);

    companion object {
        fun fromInt(value: Int?): SatisfactionDisplayType? {
            return if (value == null) {
                null
            } else {
                entries.firstOrNull { it.value == value }
            }
        }
    }
}