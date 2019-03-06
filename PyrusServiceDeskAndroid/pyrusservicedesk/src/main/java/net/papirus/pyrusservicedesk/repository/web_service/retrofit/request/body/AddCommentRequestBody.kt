package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.Attachment

internal class AddCommentRequestBody(
        appId: String,
        userId: String,
        @SerializedName("comment")
        val comment: String? = null,
        @SerializedName("attachments")
        val attachments: List<Attachment>? = null)
    : RequestBodyBase(appId, userId)
