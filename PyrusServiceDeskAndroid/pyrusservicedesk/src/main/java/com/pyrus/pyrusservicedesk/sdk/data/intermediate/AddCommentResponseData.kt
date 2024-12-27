package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto

/**
 * Response data of add comment request
 */
@Keep
internal data class AddCommentResponseData(
    @SerializedName("comment_id")
    val commentId: Long?,
    @SerializedName("attachments")
    val attachmentIds: List<Int>?,
    val sentAttachments: List<AttachmentDto>?
)