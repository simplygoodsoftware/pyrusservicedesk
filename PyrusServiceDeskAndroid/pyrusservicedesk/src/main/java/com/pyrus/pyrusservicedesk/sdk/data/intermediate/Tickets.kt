package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.Application
import com.pyrus.pyrusservicedesk.sdk.data.Ticket

/**
 * Intermediate data for parsing list of Ticket object
 */
@Keep
internal data class Tickets(
        @SerializedName("hasMore")
        val hasMore: Boolean?,
        @SerializedName("applications")
        val applications: List<Application>? = emptyList(),
        @SerializedName("tickets")
        val tickets: List<Ticket>? = emptyList())
//        @SerializedName("TicketCommandResult")
//        val additionalUsers: List<TicketCommandResult>?,