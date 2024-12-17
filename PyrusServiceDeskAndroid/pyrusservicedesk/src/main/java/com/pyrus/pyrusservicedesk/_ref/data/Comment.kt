package com.pyrus.pyrusservicedesk._ref.data

internal data class Comment(
    val id: Long,
    val isLocal: Boolean,
    val body: String?,
    val isInbound: Boolean,
    val attachments: List<Attachment>?,
    val creationTime: Long,
    val rating: Int?,
    val author: Author,
    val isSending: Boolean,
)