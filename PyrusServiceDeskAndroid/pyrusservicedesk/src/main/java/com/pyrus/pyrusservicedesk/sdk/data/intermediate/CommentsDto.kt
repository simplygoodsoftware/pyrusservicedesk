package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Intermediate data for parsing list of comments object
 */

@JsonClass(generateAdapter = true)
internal data class CommentsDto(
    @Json(name = "comments") val comments: List<CommentDto> = emptyList(),

    @Json(name = "show_rating") val showRating: Boolean = false,

    @Json(name = "show_rating_text") val showRatingText: String = ""
) {
    override fun toString(): String {
        return "Comments(comments=${comments.size}, showRating=$showRating, showRatingText='$showRatingText')"
    }
}