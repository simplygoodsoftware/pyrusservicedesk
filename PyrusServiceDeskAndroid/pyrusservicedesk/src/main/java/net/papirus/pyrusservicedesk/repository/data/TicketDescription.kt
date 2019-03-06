package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.intermediate.Attachments

internal data class TicketDescription(
    @SerializedName("subject")
    val subject: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("attachments")
    val attachments: Attachments? = null)