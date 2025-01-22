package com.pyrus.pyrusservicedesk._ref.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents ticket object.
 * @param id comment id.
 * @param isLocal flag indicating whether comment is local.
 * @param body comment text.
 * @param isInbound flag indicating whether the comment is from support.
 * @param attachments list of attachments.
 * @param creationTime comment creation date.
 * @param rating rating given by the user in the ticket.
 * @param author information about author.
 * @param isSending flag indicating whether the comment has already been sent.
 */

@JsonClass(generateAdapter = true)
internal data class Comment(
    @Json(name = "id") val id: Long,
    @Json(name = "is_local") val isLocal: Boolean,
    @Json(name = "body") val body: String?,
    @Json(name = "is_inbound") val isInbound: Boolean,
    @Json(name = "attachments") val attachments: List<Attachment>?,
    @Json(name = "creation_time") val creationTime: Long,
    @Json(name = "rating") val rating: Int?,
    @Json(name = "author") val author: Author?,
    @Json(name = "is_sending") val isSending: Boolean,
    // TODO "client_id" ??
)