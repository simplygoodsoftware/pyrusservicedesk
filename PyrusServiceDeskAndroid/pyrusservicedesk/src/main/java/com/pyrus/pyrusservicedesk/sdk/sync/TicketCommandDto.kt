package com.pyrus.pyrusservicedesk.sdk.sync

import com.google.gson.annotations.SerializedName

internal data class TicketCommandDto(
    @SerializedName("command_id") val commandId: String,
    @SerializedName("type") val type: Int,
    @SerializedName("params") val params: CommandParamsDto,
)