package net.papirus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.sdk.data.Comment

/**
 * Intermediate data for parsing list of comments object
 */
internal data class Comments(
    @SerializedName("comments")
    val comments: List<Comment> = emptyList())