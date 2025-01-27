package com.pyrus.pyrusservicedesk._ref.data

internal data class TicketHeader(
    val subject: String?,
    val isRead: Boolean,
    val lastComment: LastComment?,
    val isActive: Boolean,
    val userId: String,
    val ticketId: Long,
    val isLoading: Boolean,
) {

    data class LastComment(
        val id: Long,
        val author: Author?,
        val body: String?,
        val lastAttachmentName: String?,
        val creationDate: Long
    )

}