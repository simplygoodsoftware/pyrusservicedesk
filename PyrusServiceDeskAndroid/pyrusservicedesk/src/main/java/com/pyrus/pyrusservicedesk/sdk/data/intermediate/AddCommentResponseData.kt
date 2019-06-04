package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.Attachment

/**
 * Response data of add comment request
 */
internal data class AddCommentResponseData(
    @SerializedName("comment_id")
    val commentId: Int,
    @SerializedName("attachments")
    val attachmentIds: List<Int>?,
    val sentAttachments: List<Attachment>?)