package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto

/**
 * Intermediate data for parsing list of comments object
 */
@Keep
internal data class Comments(
    @SerializedName("comments")
    val comments: List<CommentDto> = emptyList(),

    @SerializedName("show_rating")
    val showRating: Boolean = false,

    @SerializedName("show_rating_text")
    val showRatingText: String = ""
) {
    override fun toString(): String {
        return "Comments(comments=${comments.size}, showRating=$showRating, showRatingText='$showRatingText')"
    }
}