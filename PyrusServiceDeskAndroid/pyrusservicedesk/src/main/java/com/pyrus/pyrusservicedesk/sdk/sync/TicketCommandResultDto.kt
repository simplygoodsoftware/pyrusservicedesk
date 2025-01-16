package com.pyrus.pyrusservicedesk.sdk.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents an command of the comment.
 */
@JsonClass(generateAdapter = true)
internal data class TicketCommandResultDto(
    @Json(name = "command_id") val commandId: String?,
    @Json(name = "comment_id") val commentId: Long?,
    @Json(name = "ticket_id") val ticketId: Long?,
    @Json(name = "error") val error: ErrorDto?,
)