package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response data of add comment request
 */

@JsonClass(generateAdapter = true)
internal data class AddCommentResponseData(
    @Json(name = "comment_id")
    val commentId: Long?,
    @Json(name = "attachments")
    val attachmentIds: List<Long>?,
    val sentAttachments: List<AttachmentDto>?
)