package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.Application
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.TicketCommandResult

/**
 * Intermediate data for parsing list of Ticket object
 */
@Keep
internal data class TicketsDto(
    @SerializedName("hasMore") val hasMore: Boolean?,
    @SerializedName("applications") val applications: List<Application>? = emptyList(),
    @SerializedName("tickets") val tickets: List<Ticket>? = emptyList(),
    @SerializedName("commands_result") val commandsResult: List<TicketCommandResult>?,
)