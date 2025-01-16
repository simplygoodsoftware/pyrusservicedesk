package com.pyrus.pyrusservicedesk.sdk.repositories

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CommandEntity(
    @Json(name = "is_error") val isError: Boolean,
    @Json(name = "local_id") val localId: Long,
    @Json(name = "command_id") val commandId: String,
    @Json(name = "command_type") val commandType: Int,
    @Json(name = "user_id") val userId: String,
    @Json(name = "app_id") val appId: String,
    @Json(name = "creation_time") val creationTime: Long,
    @Json(name = "request_new_ticket") val requestNewTicket: Boolean?,
    @Json(name = "comment") val comment: String?,
    @Json(name = "attachments") val attachments: List<AttachmentEntity>?,
    @Json(name = "ticket_id") val ticketId: Long?,
    @Json(name = "rating") val rating: Int?,
    @Json(name = "comment_id") val commentId: Long?,
    @Json(name = "token") val token: String?,
    @Json(name = "token_type") val tokenType: String?,
)