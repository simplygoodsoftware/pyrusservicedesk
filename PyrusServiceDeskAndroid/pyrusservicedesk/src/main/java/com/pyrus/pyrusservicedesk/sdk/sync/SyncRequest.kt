package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDataDto

internal sealed interface SyncRequest {

    sealed interface Command : SyncRequest {

        val localId: Long
        val commandId: String
        val userId: String
        val appId: String
        val creationTime: Long

        data class CreateComment(
            override val localId: Long,
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            override val creationTime: Long,
            val requestNewTicket: Boolean,
            val ticketId: Long,
            val comment: String?,
            val attachments: List<Attachment>?,
            val rating: Int?,
        ) : Command

        data class SetPushToken(
            override val localId: Long,
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            override val creationTime: Long,
            val token: String,
            val tokenType: String,
        ) : Command

        data class MarkTicketAsRead(
            override val localId: Long,
            override val commandId: String,
            override val userId: String,
            override val appId: String,
            override val creationTime: Long,
            val ticketId: Long,
        ) : Command
    }

    data object Data : SyncRequest

}