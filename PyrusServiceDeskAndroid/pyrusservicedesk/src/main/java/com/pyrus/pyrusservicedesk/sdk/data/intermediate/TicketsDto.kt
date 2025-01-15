package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.pyrus.pyrusservicedesk.sdk.data.ApplicationDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Intermediate data for parsing list of Ticket object
 */
@JsonClass(generateAdapter = true)
internal data class TicketsDto(
    @Json(name = "hasMore") val hasMore: Boolean?,
    @Json(name = "applications") val applications: List<ApplicationDto>?,
    @Json(name = "tickets") val tickets: List<TicketDto>?,
    @Json(name = "commands_result") val commandsResult: List<TicketCommandResultDto>?,
)