package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.pyrus.pyrusservicedesk.sdk.data.ApplicationDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Intermediate data for parsing list of Ticket object
 * @param applications list of applications.
 * @param tickets list of ticket.
 * @param commandsResult list of commands result .
 * @param authorAccessDenied list of blocked users for current author.
 */
@JsonClass(generateAdapter = true)
internal data class TicketsDto(
    @Json(name = "applications") val applications: List<ApplicationDto>?,
    @Json(name = "tickets") val tickets: List<TicketDto>?,
    @Json(name = "commands_result") val commandsResult: List<TicketCommandResultDto>?,
    @Json(name = "author_access_denied") val authorAccessDenied: List<String>?,
)