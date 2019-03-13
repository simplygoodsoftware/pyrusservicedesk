package net.papirus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription

internal data class Tickets(
        @SerializedName("tickets")
        val tickets: List<TicketShortDescription> = emptyList())