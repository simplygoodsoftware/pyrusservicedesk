package net.papirus.pyrusservicedesk.repository.data.intermediate

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.Comment

internal data class Comments(
    @SerializedName("comments")
    val comments: List<Comment> = emptyList())