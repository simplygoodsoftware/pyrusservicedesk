package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDataDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

internal sealed interface CommandParamsDto {


    @JsonClass(generateAdapter = true)
    data class CreateComment(
        @Json(name = "request_new_ticket") val requestNewTicket: Boolean,
        @Json(name = "user_id") val userId: String,
        @Json(name = "app_id") val appId: String,
        @Json(name = "comment") val comment: String?,
        @Json(name = "attachments") val attachments: List<AttachmentDataDto>?,
        @Json(name = "ticket_id") val ticketId: Long,
        @Json(name = "rating") val rating: Int?
    ) : CommandParamsDto


    @JsonClass(generateAdapter = true)
    data class MarkTicketAsRead(
        @Json(name = "ticket_id") val ticketId: Long,
        @Json(name = "user_id") val userId: String,
        @Json(name = "app_id") val appId: String,
        @Json(name = "comment_id") val commentId: Long?, // readAll if null
    ) : CommandParamsDto


    @JsonClass(generateAdapter = true)
    data class SetPushToken(
        @Json(name = "user_id") val userId: String,
        @Json(name = "app_id") val appId: String,
        @Json(name = "type") val type: String,
        @Json(name = "token") val token: String?, // if null back will remove token from bd
    ) : CommandParamsDto


}