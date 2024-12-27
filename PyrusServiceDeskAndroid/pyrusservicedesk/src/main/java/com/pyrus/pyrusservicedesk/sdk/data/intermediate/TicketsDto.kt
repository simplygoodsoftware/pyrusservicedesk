package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.ApplicationDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto

/**
 * Intermediate data for parsing list of Ticket object
 */
@Keep
internal data class TicketsDto(
    @SerializedName("hasMore") val hasMore: Boolean?,
    @SerializedName("applications") val applications: List<ApplicationDto>?,
    @SerializedName("tickets") val tickets: List<TicketDto>?,
    @SerializedName("commands_result") val commandsResult: List<TicketCommandResultDto>?,
)