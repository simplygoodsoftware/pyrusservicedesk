package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.gson.Local
import java.util.*

internal const val COMMENT_ID_EMPTY = 0L

/**
 * Represents single comment.
 * @param isInbound  TRUE means that comment is inbound for support, not for user of the service desk.
 */
internal data class CommentDto(
    @SerializedName("comment_id") val commentId: Long = COMMENT_ID_EMPTY,
    @SerializedName("body") val body: String? = "",
    @SerializedName("is_inbound") val isInbound: Boolean = false,
    @SerializedName("attachments") val attachments: List<AttachmentDto>? = null,
    @SerializedName("created_at") val creationDate: Date,
    @SerializedName("author") val author: AuthorDto?,
    @SerializedName("rating") val rating: Int? = null,
) {

    /**
     * @return TRUE when comment contains attachments
     */
    fun hasAttachments() = !attachments.isNullOrEmpty()

    /**
     * @return TRUE if this comment is local one.
     */
    fun isLocal(): Boolean {
//        return commentId == COMMENT_ID_EMPTY && localId != COMMENT_ID_EMPTY
        return commentId == COMMENT_ID_EMPTY
    }
}