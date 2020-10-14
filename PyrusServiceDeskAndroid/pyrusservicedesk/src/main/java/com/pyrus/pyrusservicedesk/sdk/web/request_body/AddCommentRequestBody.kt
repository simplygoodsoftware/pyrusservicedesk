package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.Attachment

/**
 * Body for sending add comment request to the server. Should contain either [comment] or [attachments]
 * @param comment text of comment. Can be null
 * @param attachments list of attachments. Can be null
 * @param userName name of the sending person
 */
internal class AddCommentRequestBody(
        appId: String,
        userId: String,
        securityKey: String?,
        instanceId: String?,
        version: Int,
        @SerializedName("comment")
        val comment: String? = null,
        @SerializedName("attachments")
        val attachments: List<Attachment>? = null,
        @SerializedName("user_name")
        val userName: String,
        @SerializedName("rating")
        val rating: Int? = null
) : RequestBodyBase(appId, userId, securityKey, instanceId, version)
