package com.pyrus.pyrusservicedesk.sdk.repositories


internal data class CommandErrorEntity(
    val localId: Long,
    val commandType: Int, // TicketCommandType
    val commandId: String,
    val userId: String,
    val appId: String,
    val creationTime: Long,
    val requestNewTicket: Boolean?,
    val ticketId: Long?,
    val comment: String?,
    val attachments: List<AttachmentEntity>?,
    val rating: Int?,
    val token: String?,
    val tokenType: String?,
)