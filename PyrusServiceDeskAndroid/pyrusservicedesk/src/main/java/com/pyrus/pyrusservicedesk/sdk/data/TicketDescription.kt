package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Object that is used for sending [CreateTicketRequest]
 */
internal data class TicketDescription(
    @SerializedName("subject")
    val subject: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("attachments")
    val attachments: List<Attachment>? = null){

    /**
     * @return TRUE if this contains attachments
     */
    fun hasAttachments() = !attachments.isNullOrEmpty()
}