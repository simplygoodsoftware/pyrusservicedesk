package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.sdk.data.Attachment

internal class AddCommentRequestBody(
        appId: String,
        userId: String,
        @SerializedName("comment")
        val comment: String? = null,
        @SerializedName("attachments")
        val attachments: List<Attachment>? = null,
        @SerializedName("user_name")
        val userName: String)
    : RequestBodyBase(appId, userId)
