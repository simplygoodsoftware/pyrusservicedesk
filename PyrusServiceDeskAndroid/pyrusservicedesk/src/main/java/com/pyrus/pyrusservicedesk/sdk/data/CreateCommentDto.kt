package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CreateCommentDto(
    @Json(name = "comment") val comment: String,
    @Json(name = "request_new_ticket") val requestNewTicket: Boolean?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "app_id") val appId: String,
    @Json(name = "ticket_id") val ticketId: Int?,
    @Json(name = "attachments") val attachments: List<AttachmentDto>,
)
