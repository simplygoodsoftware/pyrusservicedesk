package com.pyrus.pyrusservicedesk.sdk.sync

import com.google.gson.annotations.SerializedName

/**
 * Represents an command of the comment.
 */
internal data class TicketCommandResultDto(
    @SerializedName("command_id") val commandId: String?,
    @SerializedName("comment_id") val commentId: Long?,
    @SerializedName("ticket_id") val ticketId: Int?,
    @SerializedName("error") val error: String?,
)