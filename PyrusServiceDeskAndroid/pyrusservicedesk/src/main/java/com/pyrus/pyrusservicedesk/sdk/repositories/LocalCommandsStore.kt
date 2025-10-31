package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Status
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.CommandsDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.SearchDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.LocalAttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithHeader
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * [SharedPreferences] based offline repository
 */
internal class LocalCommandsStore(
    private val idStore: IdStore,
    private val commandsDao: CommandsDao,
    private val searchDao: SearchDao,
) {

    private val lastLocalId: AtomicLong = AtomicLong(0)
    private val lastAttachId: AtomicLong = AtomicLong(0)

    fun getCommandsFlow(): Flow<List<CommandWithAttachmentsEntity>> = commandsDao.getCommandsFlow()

    fun getCommandsFlow(ticketId: Long): Flow<List<CommandWithAttachmentsEntity>> = commandsDao.getCommandsFlow().map {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val localTicketId = idStore.getTicketLocalId(ticketId) ?: ticketId
        it.filter { command -> command.command.ticketId == serverTicketId || command.command.ticketId == localTicketId }
    }

    fun getCommands(): List<CommandWithAttachmentsEntity> = commandsDao.getCommands()

    fun getCommands(ticketId: Long): List<CommandWithAttachmentsEntity> {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val localTicketId = idStore.getTicketLocalId(ticketId) ?: ticketId

        return commandsDao.getCommands().filter {
            it.command.ticketId == serverTicketId || it.command.ticketId == localTicketId
        }
    }

    fun addTextCommand(
        user: UserInternal,
        ticketId: Long,
        comment: String,
        instanceId: String,
    ): SyncRequest.Command.CreateComment {
        val entity = createTextCommandEntity(user, ticketId, comment)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = if (user.userId == instanceId) null else user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = false,
            ticketId = ticketId,
            comment = comment,
            attachments = null,
            rating = null,
            ratingComment = null,
            extraFields = entity.extraFields,
        )
    }

    fun addAttachmentCommand(
        user: UserInternal,
        ticketId: Long,
        fileData: FileData,
        instanceId: String,
    ): CommandWithAttachmentsEntity {
        val entity = createLocalAttachComment(user, ticketId, fileData, instanceId)
        addOrUpdatePendingCommand(entity)

        return entity
    }

    fun addRatingCommand(
        user: UserInternal,
        ticketId: Long,
        rating: Int?,
        ratingComment: String?,
        instanceId: String,
    ): SyncRequest.Command.CreateComment {
        val entity = createRatingCommandEntity(user, ticketId, rating, ratingComment)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = if (user.userId == instanceId) null else user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = false,
            ticketId = ticketId,
            comment = null,
            attachments = null,
            rating = rating,
            ratingComment = ratingComment,
            extraFields = null,
        )
    }

    fun addReadCommand(
        user: UserInternal,
        ticketId: Long,
        instanceId: String,
    ): SyncRequest.Command.MarkTicketAsRead {
        val entity = createReadCommandEntity(user, ticketId)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.MarkTicketAsRead(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = if (user.userId == instanceId) null else user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            ticketId = ticketId,
        )
    }


    // TODO
    fun createPushTokenCommand(
        user: UserInternal,
        token: String,
        tokenType: String,
        instanceId: String,
    ) = SyncRequest.Command.SetPushToken(
        localId = getNextLocalId(),
        commandId = createCommandId(),
        userId = if (user.userId == instanceId) null else user.userId,
        appId = user.appId,
        creationTime = System.currentTimeMillis(),
        token = token,
        tokenType = tokenType
    )

    fun updateCommandsTicketId(localId: Long, serverId: Long) {
        commandsDao.updateTicketId(localId, serverId)
    }

    /**
     * Adds command to store
     */
    fun addOrUpdatePendingCommand(command: CommandEntity) {
        commandsDao.insertCommand(command)
    }

    /**
     * Adds command to store
     */
    fun addOrUpdatePendingCommand(commandWithAttachments: CommandWithAttachmentsEntity) {
        commandsDao.insertCommand(commandWithAttachments)
    }

    fun getCommand(localId: Long): CommandWithAttachmentsEntity? {
        return commandsDao.getCommand(localId)
    }

    /**
     * Removes pending command from offline repository
     */
    fun removeCommand(commandId: String) {
        commandsDao.deleteCommandByCommandId(commandId)
    }

    /**
     * Removes pending command from offline repository
     */
    fun removeCommand(localId: Long) {
        commandsDao.deleteCommandByLocalId(localId)
    }

    fun searchComments(query: String, limit: Int): List<CommandWithHeader> {
        return searchDao.searchCommentsCommandsWithHeader(query, limit)
    }

    private fun createTextCommandEntity(
        user: UserInternal,
        ticketId: Long,
        comment: String,
    ): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            isError = false,
            localId = localId,
            commandId = createCommandId(),
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = comment,
            ticketId = ticketId,
            rating = null,
            commentId = localId,
            token = null,
            tokenType = null,
            ratingComment = null,
            extraFields = ConfigUtils.getFieldsData(),
        )
    }

    private fun createRatingCommandEntity(
        user: UserInternal,
        ticketId: Long,
        rating: Int?,
        ratingComment: String?,
    ): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            isError = false,
            localId = localId,
            commandId = createCommandId(),
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = null,
            ticketId = ticketId,
            rating = rating,
            commentId = localId,
            token = null,
            tokenType = null,
            ratingComment = ratingComment,
            extraFields = null,
        )
    }

    private fun createLocalAttachComment(
        user: UserInternal,
        ticketId: Long,
        fileData: FileData,
        instanceId: String,
    ): CommandWithAttachmentsEntity {
        val localId = getNextLocalId()
        val commandEntity = CommandEntity(
            isError = false,
            localId = localId,
            commandId = createCommandId(),
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = if (user.userId == instanceId) null else user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = null,
            ticketId = ticketId,
            rating = null,
            commentId = localId,
            token = null,
            tokenType = null,
            ratingComment = null,
            extraFields = ConfigUtils.getFieldsData(),
        )
        val attachmentEntities = listOf(createAttachment(commandEntity.commandId, fileData))

        return CommandWithAttachmentsEntity(commandEntity, attachmentEntities)
    }

    private fun createAttachment(commandId: String, fileData: FileData): LocalAttachmentEntity {
        return LocalAttachmentEntity(
            id = getNextAttachmentId(),
            commandId= commandId,
            name = fileData.fileName,
            guid = null,
            bytesSize = fileData.bytesSize,
            uri = fileData.uri.toString(),
            status = Status.Processing.ordinal,
            progress = null
        )
    }

    private fun createReadCommandEntity(user: UserInternal, ticketId: Long): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            isError = false,
            localId = localId,
            commandId = createCommandId(),
            commandType = TicketCommandType.MarkTicketAsRead.ordinal,
            userId = user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = null,
            ticketId = ticketId,
            rating = null,
            commentId = null,
            token = null,
            tokenType = null,
            ratingComment = null,
            extraFields = null,
        )
    }

    fun getNextLocalId(): Long {
        return lastLocalId.getAndUpdate {
            if (it == 0L) {
                val minId = commandsDao.getCommandMinLocalId()
                if (minId == null) -1
                else minId - 1
            }
            else {
                it - 1
            }
        }
    }

    private fun getNextAttachmentId(): Long {
        return lastAttachId.getAndUpdate {
            if (it == 0L) {
                val minId = commandsDao.getAttachmentMinLocalId()
                if (minId == null) -1
                else minId - 1
            }
            else {
                it - 1
            }
        }
    }

    private fun createCommandId(): String {
        return UUID.randomUUID().toString()
    }

}