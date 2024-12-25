package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk._ref.data.Attachment

internal sealed interface SyncRequest {

    sealed interface Command : SyncRequest {

        val commandId: String
        val userId: String
        val appId: String

        data class CreateTicket(
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            val ticketId: Int,
            val comment: String?,
            val attachments: List<Attachment>?,
            val rating: Int?,
        ) : Command

        data class AddComment(
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            val ticketId: Int,
            val comment: String?,
            val attachments: List<Attachment>?,
            val rating: Int?,
        ) : Command

        data class SetPushToken(
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            val token: String,
            val tokenType: String,
        ) : Command

        data class MarkTicketAsRead(
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            val ticketId: Int,
        ) : Command
    }

    data object Data : SyncRequest

}