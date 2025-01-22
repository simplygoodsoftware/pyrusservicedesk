package com.pyrus.pyrusservicedesk.sdk.data

import com.pyrus.pyrusservicedesk.sdk.data.json.DateJ
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

internal const val COMMENT_ID_EMPTY = 0L

/**
 * Represents single comment.
 * @param commentId comment id.
 * @param body comment text.
 * @param isInbound TRUE means that comment is inbound for support, not for user of the service desk.
 * @param attachments list of attachments.
 * @param creationDate comment creation date.
 * @param rating rating given by the user in the ticket.
 * @param author information about author.
 */

@JsonClass(generateAdapter = true)
internal data class CommentDto(
    @Json(name = "comment_id") val commentId: Long = COMMENT_ID_EMPTY,
    @Json(name = "body") val body: String? = "",
    @Json(name = "is_inbound") val isInbound: Boolean = false,
    @Json(name = "attachments") val attachments: List<AttachmentDto>? = null,
    @Json(name = "created_at") @DateJ val creationDate: Long,
    @Json(name = "author") val author: AuthorDto?,
    @Json(name = "rating") val rating: Int? = null,
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