package net.papirus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.sdk.data.Comment

internal data class Comments(
    @SerializedName("comments")
    val comments: List<Comment> = emptyList())