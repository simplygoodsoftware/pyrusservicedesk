package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * [SharedPreferences] based offline repository
 */
internal class LocalCommandsStore(
    private val preferences: SharedPreferences,
    private val localDataVerifier: LocalDataVerifier,
    private val idStore: IdStore,
) {

    private val lastLocalId: AtomicLong
    private val lastAttachId: AtomicLong

    private val commandsStateFlow: MutableStateFlow<List<CommandEntity>>

    init {
        val pendingCommands = readCommands()

        var lastCommandId = 0L
        var lastAttachmentId = 0L
        for (command in pendingCommands) {
            lastCommandId = max(lastCommandId, command.localId)
            if (command.attachments != null) {
                for (attach in command.attachments) {
                    lastAttachmentId = max(lastAttachmentId, attach.id)
                }
            }
        }
        lastLocalId = AtomicLong(lastCommandId)
        lastAttachId = AtomicLong(lastAttachmentId)

        commandsStateFlow = MutableStateFlow(pendingCommands)
    }

    fun getCommandsFlow(): Flow<List<CommandEntity>> = commandsStateFlow

    fun getCommandsFlow(ticketId: Long): Flow<List<CommandEntity>> = commandsStateFlow.map {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val localTicketId = idStore.getTicketLocalId(ticketId) ?: ticketId
        it.filter { command -> command.ticketId == serverTicketId || command.ticketId == localTicketId }
    }

    fun getCommands(): List<CommandEntity> = commandsStateFlow.value

    fun getCommands(ticketId: Long): List<CommandEntity> {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        val localTicketId = idStore.getTicketLocalId(ticketId) ?: ticketId
        return commandsStateFlow.value.filter {
            it.ticketId == serverTicketId || it.ticketId == localTicketId
        }
    }

    fun addTextCommand(
        user: UserInternal,
        ticketId: Long,
        requestNewTicket: Boolean,
        comment: String,
    ): SyncRequest.Command.CreateComment {
        val entity = createTextCommandEntity(user, ticketId, requestNewTicket, comment)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = requestNewTicket,
            ticketId = ticketId,
            comment = comment,
            attachments = null,
            rating = null
        )
    }

    fun addAttachmentCommand(
        user: UserInternal,
        ticketId: Long,
        requestNewTicket: Boolean,
        fileData: FileData,
    ): CommandEntity {
        val entity = createLocalAttachComment(user, ticketId, requestNewTicket, fileData)
        addOrUpdatePendingCommand(entity)
        return entity
    }

    fun addRatingCommand(
        user: UserInternal,
        ticketId: Long,
        requestNewTicket: Boolean,
        rating: Int,
    ): SyncRequest.Command.CreateComment {
        val entity = createRatingCommandEntity(user, ticketId, requestNewTicket, rating)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = requestNewTicket,
            ticketId = ticketId,
            comment = null,
            attachments = null,
            rating = rating
        )
    }

    fun addReadCommand(
        user: UserInternal,
        ticketId: Long,
    ): SyncRequest.Command.MarkTicketAsRead {
        val entity = createReadCommandEntity(user, ticketId)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.MarkTicketAsRead(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = user.userId,
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
    ) = SyncRequest.Command.SetPushToken(
        localId = getNextLocalId(),
        commandId = createCommandId(),
        userId = user.userId,
        appId = user.appId,
        creationTime = System.currentTimeMillis(),
        token = token,
        tokenType = tokenType
    )

    /**
     * Adds command to store
     */
    fun addOrUpdatePendingCommand(command: CommandEntity) {
        var commands = commandsStateFlow.value.toMutableList()

        commands.let { list ->
            val existingIndex = list.indexOfFirst { it.commandId == command.commandId }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
            }
            list.add(command)
        }
        if (commands.size > MAX_PENDING_COMMENTS_SIZE) {
            commands = commands.subList(commands.size - MAX_PENDING_COMMENTS_SIZE, commands.size)
        }
        writeCommands(commands)
    }


    /**
     * Provides all pending feed commands
     */
    private fun readCommands(): List<CommandEntity> {
//        val rawJson = preferences.getString(PREFERENCE_KEY_TICKET_COMMANDS, "[]")
//        val commandsList = gson.fromJson<List<CommandEntity>>(rawJson, commandListTokenType).toMutableList()
//
//        if (commandsList.removeAll { localDataVerifier.isLocalCommandEmpty(it) }) {
//            writeCommands(commandsList)
//        }
        // TODO()
        return emptyList()
    }

    fun getCommand(localId: Long): CommandEntity? {
        return commandsStateFlow.value.find { command -> command.localId == localId }
    }

    /**
     * Removes pending command from offline repository
     */
    fun removeCommand(commandId: String) {
        val commands = getCommands().toMutableList()
        val removed = commands.removeAll { it.commandId == commandId }
        if (removed) {
            writeCommands(commands)
        }
    }

    /**
     * Removes all commands from offline repository
     */
    fun removeAllCommands() {
        writeCommands(emptyList())
    }

    fun getNextLocalId(): Long {
        return lastLocalId.decrementAndGet()
    }

    private fun createNewTicketCommandEntity(
        user: UserInternal,
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
            requestNewTicket = true,
            comment = comment,
            attachments = null,
            ticketId = null,
            rating = null,
            commentId = localId,
            token = null,
            tokenType = null,
        )
    }

    private fun createTextCommandEntity(
        user: UserInternal,
        ticketId: Long,
        requestNewTicket: Boolean,
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
            requestNewTicket = requestNewTicket,
            comment = comment,
            attachments = null,
            ticketId = ticketId,
            rating = null,
            commentId = localId,
            token = null,
            tokenType = null,
        )
    }

    private fun createRatingCommandEntity(
        user: UserInternal,
        ticketId: Long,
        requestNewTicket: Boolean,
        rating: Int,
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
            requestNewTicket = requestNewTicket,
            comment = null,
            attachments = null,
            ticketId = ticketId,
            rating = rating,
            commentId = localId,
            token = null,
            tokenType = null,
        )
    }

    private fun createLocalAttachComment(
        user: UserInternal,
        ticketId: Long,
        requestNewTicket: Boolean,
        fileData: FileData,
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
            requestNewTicket = requestNewTicket,
            comment = null,
            attachments = listOf(createAttachment(fileData)),
            ticketId = ticketId,
            rating = null,
            commentId = localId,
            token = null,
            tokenType = null,
        )
    }

    private fun createAttachment(fileData: FileData): AttachmentEntity {
        return AttachmentEntity(
            id = getNextAttachmentId(),
            name = fileData.fileName,
            guid = null,
            bytesSize = fileData.bytesSize,
            uri = fileData.uri,
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
            attachments = null,
            ticketId = ticketId,
            rating = null,
            commentId = null,
            token = null,
            tokenType = null,
        )
    }

    private fun getNextAttachmentId(): Long {
        return lastAttachId.decrementAndGet()
    }

    private fun createCommandId(): String {
        return UUID.randomUUID().toString()
    }

    private fun writeCommands(commands: List<CommandEntity>) {
//        val rawJson = gson.toJson(commands, commandListTokenType)
//        preferences.edit().putString(PREFERENCE_KEY_TICKET_COMMANDS, rawJson).apply()
        commandsStateFlow.value = commands
    }

    private companion object{
        const val PREFERENCE_KEY_TICKET_COMMANDS = "PREFERENCE_KEY_TICKET_COMMANDS"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commandListTokenType: Type = object : TypeToken<List<CommandEntity>>(){}.type
    }
}