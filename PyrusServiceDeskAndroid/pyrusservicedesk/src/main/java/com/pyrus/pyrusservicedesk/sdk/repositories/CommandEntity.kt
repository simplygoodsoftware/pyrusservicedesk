package com.pyrus.pyrusservicedesk.sdk.repositories

import com.google.gson.annotations.SerializedName

internal data class CommandEntity(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("command_id") val commandId: String,
    @SerializedName("command_type") val commandType: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("app_id") val appId: String,
    @SerializedName("creation_time") val creationTime: Long,
    @SerializedName("request_new_ticket") val requestNewTicket: Boolean?,
    @SerializedName("comment") val comment: String?,
    @SerializedName("attachments") val attachments: List<AttachmentEntity>?,
    @SerializedName("ticket_id") val ticketId: Long?,
    @SerializedName("rating") val rating: Int?,
    @SerializedName("comment_id") val commentId: Long?,
)