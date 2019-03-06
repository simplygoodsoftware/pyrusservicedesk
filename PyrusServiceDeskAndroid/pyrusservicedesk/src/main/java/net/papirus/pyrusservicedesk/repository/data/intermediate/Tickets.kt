package net.papirus.pyrusservicedesk.repository.data.intermediate

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.Ticket

internal data class Tickets(
        @SerializedName("Tickets")
        val tickets: List<Ticket> = emptyList())