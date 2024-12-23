package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Represents an command of the comment.
 */
data class TicketCommandResultDto(
    @SerializedName("command_id") val commandId: String,
    @SerializedName("comment_id") val commentId: Long,
    @SerializedName("ticket_id") val ticketId: Int,
    @SerializedName("error") val error: String, //TODO
)