package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal data class CreateComment(
    @SerializedName("comment")
    val comment: String,
    @SerializedName("request_new_ticket")
    val requestNewTicket: Boolean?,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("app_id")
    val appId: String,
    @SerializedName("ticket_id")
    val ticketId: Int?,
    @SerializedName("attachments")
    val attachments: List<AttachmentDto>,
)
