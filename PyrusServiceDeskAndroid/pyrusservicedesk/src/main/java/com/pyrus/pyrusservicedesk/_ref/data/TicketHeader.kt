package com.pyrus.pyrusservicedesk._ref.data

internal data class TicketHeader(
    val subject: String?,
    val isRead: Boolean,
    val lastCommentText: String?,
    val lastCommentCreationDate: Long?,
    val isActive: Boolean?,
    val userId: String,
    val ticketId: Long,
    val isLoading: Boolean,
)