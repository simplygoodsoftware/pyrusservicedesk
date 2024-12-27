package com.pyrus.pyrusservicedesk.sdk.sync

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDataDto

internal sealed interface CommandParamsDto {

    data class CreateComment(
        @SerializedName("request_new_ticket") val requestNewTicket: Boolean,
        @SerializedName("user_id") val userId: String,
        @SerializedName("app_id") val appId: String,
        @SerializedName("comment") val comment: String?,
        @SerializedName("attachments") val attachments: List<AttachmentDataDto>?,
        @SerializedName("ticket_id") val ticketId: Int,
        @SerializedName("rating") val rating: Int?
    ) : CommandParamsDto

    data class MarkTicketAsRead(
        @SerializedName("ticket_id") val ticketId: Int,
        @SerializedName("user_id") val userId: String,
        @SerializedName("app_id") val appId: String,
        @SerializedName("comment_id") val commentId: Long?, // readAll if null
    ) : CommandParamsDto

    data class SetPushToken(
        @SerializedName("user_id") val userId: String,
        @SerializedName("app_id") val appId: String,
        @SerializedName("type") val type: String,
        @SerializedName("token") val token: String?, // if null back will remove token from bd
    ) : CommandParamsDto


}