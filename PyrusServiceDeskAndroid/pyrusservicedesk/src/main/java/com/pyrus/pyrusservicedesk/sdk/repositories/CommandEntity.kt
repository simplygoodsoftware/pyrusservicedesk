package com.pyrus.pyrusservicedesk.sdk.repositories

import com.google.gson.annotations.SerializedName

internal data class CommandEntity(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("command_id") val commandId: String,
    @SerializedName("command_type") val commandType: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("app_id") val appId: String,
    @SerializedName("request_new_ticket") val requestNewTicket: Boolean?,
    @SerializedName("comment") val comment: String?,
    @SerializedName("attachments") val attachments: List<AttachmentEntity>?,
    @SerializedName("ticket_id") val ticketId: Int?,
    @SerializedName("rating") val rating: Int?,
    @SerializedName("comment_id") val commentId: Long?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("token") val token: String?,
)