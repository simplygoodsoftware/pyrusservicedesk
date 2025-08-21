package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMANDS_TABLE

@Entity(
    tableName = COMMANDS_TABLE,
    primaryKeys = ["command_id", "local_id"],
    indices = [
        Index(value = ["command_id"], unique = true),
        Index(value = ["local_id"], unique = true),
    ],
)
internal data class CommandEntity(
    @ColumnInfo(name = "is_error") val isError: Boolean,
    @ColumnInfo(name = "local_id") val localId: Long,
    @ColumnInfo(name = "command_id") val commandId: String,
    @ColumnInfo(name = "command_type") val commandType: Int,
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "creation_time") val creationTime: Long,
    @ColumnInfo(name = "request_new_ticket") val requestNewTicket: Boolean?,
    @ColumnInfo(name = "comment") val comment: String?,
    @ColumnInfo(name = "ticket_id") val ticketId: Long?,
    @ColumnInfo(name = "rating") val rating: Int?,
    @ColumnInfo(name = "comment_id") val commentId: Long?,
    @ColumnInfo(name = "token") val token: String?,
    @ColumnInfo(name = "token_type") val tokenType: String?,
) : TicketHeaderEntity