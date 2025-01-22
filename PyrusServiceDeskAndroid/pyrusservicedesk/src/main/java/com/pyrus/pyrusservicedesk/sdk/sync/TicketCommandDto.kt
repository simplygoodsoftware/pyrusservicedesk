package com.pyrus.pyrusservicedesk.sdk.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @param commandId command guid.
 * @param type command type TicketCommandType.
 * @param appId extension id.
 * @param userId user id.
 * @param params map of task modification parameters. Parameters depend on the value of type.
 */

@JsonClass(generateAdapter = true)
internal data class TicketCommandDto(
    @Json(name = "command_id") val commandId: String,
    @Json(name = "type") val type: Int, // TicketCommandType
    @Json(name = "app_id") val appId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "params") val params: CommandParamsDto,
)