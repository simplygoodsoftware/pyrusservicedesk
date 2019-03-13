package net.papirus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal data class TicketDescription(
    @SerializedName("subject")
    val subject: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("attachments")
    val attachments: List<Attachment>? = null)