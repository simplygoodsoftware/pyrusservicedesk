package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription

/**
 * Intermediate data for parsing list of TicketShortDescription object
 */
@Keep
internal data class Tickets(
    @SerializedName("tickets") val tickets: List<TicketShortDescription> = emptyList(),
)