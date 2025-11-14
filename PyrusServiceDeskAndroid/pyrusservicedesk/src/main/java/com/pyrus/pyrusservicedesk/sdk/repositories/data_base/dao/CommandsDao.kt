package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.ATTACHMENTS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMANDS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.LocalAttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class CommandsDao {

    @Transaction
    @Query("SELECT * FROM $COMMANDS_TABLE WHERE local_id = :localId")
    abstract fun getCommand(localId: Long): CommandWithAttachmentsEntity?

    @Transaction
    @Query("SELECT * FROM $COMMANDS_TABLE")
    abstract fun getCommands(): List<CommandWithAttachmentsEntity>

    @Transaction
    @Query("SELECT * FROM $COMMANDS_TABLE")
    abstract fun getCommandsFlow(): Flow<List<CommandWithAttachmentsEntity>>

    @Query("DELETE FROM $COMMANDS_TABLE WHERE command_id = :commandId")
    abstract fun deleteCommandByCommandId(commandId: String)

    @Query("DELETE FROM $COMMANDS_TABLE WHERE local_id = :localId")
    abstract fun deleteCommandByLocalId(localId: Long)

    @Query("SELECT min(local_id) FROM $COMMANDS_TABLE")
    abstract fun getCommandMinLocalId(): Long?

    @Query("SELECT min(id) FROM $ATTACHMENTS_TABLE")
    abstract fun getAttachmentMinLocalId(): Long?

    @Transaction
    open fun insertCommand(commandWithAttachments: CommandWithAttachmentsEntity) {
        val command = commandWithAttachments.command
        val attachments = commandWithAttachments.attachments

        insertCommand(command)
        attachments?.let(::insertAttachments)
    }

    @Query("UPDATE $COMMANDS_TABLE SET ticket_id = :serverId WHERE ticket_id = :localId")
    abstract fun updateTicketId(localId: Long, serverId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertCommand(commandEntity: CommandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAttachments(attachments: List<LocalAttachmentEntity>)

}