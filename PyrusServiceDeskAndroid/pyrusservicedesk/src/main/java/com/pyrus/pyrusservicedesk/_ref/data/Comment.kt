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

internal data class Comment(
    val id: Long,
    val isLocal: Boolean,
    val body: String?,
    val isInbound: Boolean,
    val isSupport: Boolean,
    val attachments: List<Attachment>?,
    val creationTime: Long,
    val rating: Int?,
    val author: Author?,
    val isSending: Boolean,
)