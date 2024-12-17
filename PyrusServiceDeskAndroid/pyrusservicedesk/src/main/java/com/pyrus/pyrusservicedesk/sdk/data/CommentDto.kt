package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.gson.Local
import java.util.*

internal const val COMMENT_ID_EMPTY = 0L

/**
 * Represents single comment.
 * @param isInbound  TRUE means that comment is inbound for support, not for user of the service desk.
 * @param localId id of local comment. For server comments this is always [COMMENT_ID_EMPTY]
 */
internal data class CommentDto(
    @SerializedName("comment_id") val commentId: Long = COMMENT_ID_EMPTY,
    @SerializedName("body") val body: String? = "",
    @SerializedName("is_inbound") val isInbound: Boolean = false,
    @SerializedName("attachments") val attachments: List<AttachmentDto>? = null,
    @SerializedName("created_at") val creationDate: Date,
    @SerializedName("author") val author: Author,
    @Local @SerializedName("local_comment_id") val localId: Long = COMMENT_ID_EMPTY,
    @SerializedName("rating") val rating: Int? = null,
    @Transient val isSending: Boolean = false,
) {

    /**
     * @return TRUE when comment contains attachments
     */
    fun hasAttachments() = !attachments.isNullOrEmpty()

    /**
     * @return TRUE if this comment is local one.
     */
    fun isLocal() = commentId == COMMENT_ID_EMPTY && localId != COMMENT_ID_EMPTY
}