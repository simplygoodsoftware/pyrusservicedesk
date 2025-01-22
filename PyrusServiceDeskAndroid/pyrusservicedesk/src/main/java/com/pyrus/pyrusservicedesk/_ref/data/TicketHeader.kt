package com.pyrus.pyrusservicedesk._ref.data

import com.pyrus.pyrusservicedesk._ref.utils.TextProvider

internal data class TicketHeader(
    val subject: String?,
    val isRead: Boolean,
    val lastCommentText: TextProvider?,
    val lastCommentCreationDate: Long?,
    val isActive: Boolean?,
    val userId: String,
    val ticketId: Long,
    val isLoading: Boolean,
)