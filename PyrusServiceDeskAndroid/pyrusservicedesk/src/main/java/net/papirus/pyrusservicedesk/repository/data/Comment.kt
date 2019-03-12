package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName
import java.util.*

internal data class Comment(
        @SerializedName("comment_id")
        val commentId: Int = 0,
        @SerializedName("body")
        val body: String = "",
        // inbound for support, not for user
        @SerializedName("is_inbound")
        val isInbound: Boolean = false,
        @SerializedName("attachments")
        val attachments: List<Attachment>? = null,
        @SerializedName("created_at")
        val creationDate: Date,
        @SerializedName("author")
        val author: Author){

    fun hasAttachments() = attachments != null
}