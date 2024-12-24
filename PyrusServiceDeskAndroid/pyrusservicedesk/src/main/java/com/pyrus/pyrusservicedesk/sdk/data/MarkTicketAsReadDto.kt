package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal data class MarkTicketAsReadDto(
    @SerializedName("ticket_id") val ticketId: String?,
    @SerializedName("user_id") val userId: String,
    @SerializedName("app_id") val appId: String,
)
