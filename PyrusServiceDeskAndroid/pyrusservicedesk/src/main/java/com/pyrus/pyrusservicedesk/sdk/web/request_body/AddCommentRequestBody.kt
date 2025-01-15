package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body for sending add comment request to the server. Should contain either [comment] or [attachments]
 * @param comment text of comment. Can be null
 * @param attachments list of attachments. Can be null
 * @param userName name of the sending person
 */

@JsonClass(generateAdapter = true)
internal class AddCommentRequestBody(
    appId: String,
    userId: String,
    securityKey: String?,
    instanceId: String?,
    version: Int,
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "attachments") val attachments: List<AttachmentDto>? = null,
    @Json(name = "user_name") val userName: String,
    @Json(name = "rating") val rating: Int? = null,
    @Json(name = "extra_fields") val extraFields: Map<String, String>?,
) : RequestBodyBase(true, null, null, null, null, null, appId, userId, securityKey, instanceId, version, null)
