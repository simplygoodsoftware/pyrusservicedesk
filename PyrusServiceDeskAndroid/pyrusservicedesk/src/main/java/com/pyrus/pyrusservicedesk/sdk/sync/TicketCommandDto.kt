package com.pyrus.pyrusservicedesk.sdk.sync

import com.google.gson.annotations.SerializedName

internal data class TicketCommandDto(
    @SerializedName("command_Id") val commandId: String,
    @SerializedName("type") val type: TicketCommandType,
    @SerializedName("params") val params: CommandParamsDto,
)