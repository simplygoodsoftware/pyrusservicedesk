package com.pyrus.pyrusservicedesk.sdk.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
internal data class TicketCommandDto(
    @Json(name = "command_id") val commandId: String,
    @Json(name = "type") val type: Int, // TicketCommandType
    @Json(name = "app_id") val appId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "params") val params: CommandParamsDto,
)