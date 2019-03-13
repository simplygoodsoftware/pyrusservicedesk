package net.papirus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName
import java.util.*

private const val COMMENT_ID_EMPTY = 0

internal data class Comment(
        @SerializedName("comment_id")
        val commentId: Int = COMMENT_ID_EMPTY,
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
        val author: Author,
        @Transient
        val localId: Int = COMMENT_ID_EMPTY){

    fun hasAttachments() = attachments != null

    fun isLocal() = commentId == COMMENT_ID_EMPTY && localId != COMMENT_ID_EMPTY
}