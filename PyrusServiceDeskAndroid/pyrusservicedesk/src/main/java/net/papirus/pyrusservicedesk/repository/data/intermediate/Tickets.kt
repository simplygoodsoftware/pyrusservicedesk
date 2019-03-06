package net.papirus.pyrusservicedesk.repository.data.intermediate

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.TicketShortDescription

internal data class Tickets(
        @SerializedName("tickets")
        val tickets: List<TicketShortDescription> = emptyList())