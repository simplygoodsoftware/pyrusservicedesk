package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.Comment

/**
 * Intermediate data for parsing list of comments object
 */
@Keep
internal data class Comments(
    @SerializedName("comments")
    val comments: List<Comment> = emptyList(),

    @SerializedName("show_rating")
    val showRating: Boolean = false,

    @SerializedName("show_rating_text")
    val showRatingText: String = "",

    @SerializedName("welcome_message")
    val welcomeMessage: String? = null,

    @SerializedName("rating_settings")
    val ratingSettings: RatingSettings? = null,
)