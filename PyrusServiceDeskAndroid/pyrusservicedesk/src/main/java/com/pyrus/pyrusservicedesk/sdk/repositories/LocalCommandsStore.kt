package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * [SharedPreferences] based offline repository
 */
internal class LocalCommandsStore(
    private val preferences: SharedPreferences,
    private val localDataVerifier: LocalDataVerifier,
    private val gson: Gson,
) {

    private val lastLocalId: AtomicLong
    private val lastAttachId: AtomicLong

    private val localCommandsStateFlow: MutableStateFlow<List<CommandEntity>>
    private val commandErrorsStateFlow: MutableStateFlow<List<CommandErrorEntity>>

    init {
        val pendingCommands = readPendingCommands()
        val commandErrors = readCommandErrors()

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
        for (commandError in commandErrors) {
            lastCommandId = max(lastCommandId, commandError.localId)
            if (commandError.attachments != null) {
                for (attach in commandError.attachments) {
                    lastAttachmentId = max(lastAttachmentId, attach.id)
                }
            }
        }
        lastLocalId = AtomicLong(lastCommandId)
        lastAttachId = AtomicLong(lastAttachmentId)

        localCommandsStateFlow = MutableStateFlow(pendingCommands)
        commandErrorsStateFlow = MutableStateFlow(commandErrors)
    }

    val commandsFlow: Flow<List<CommandEntity>> = localCommandsStateFlow

    fun addCreateTicketCommand(user: UserInternal, comment: String): SyncRequest.Command.CreateComment {
        val entity = createNewTicketCommandEntity(user, comment)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = true, // TODO
            ticketId = entity.localId,
            comment = comment,
            attachments = null,
            rating = null
        )
    }

    fun addTextCommand(
        user: UserInternal,
        ticketId: Long,
        comment: String,
    ): SyncRequest.Command.CreateComment {
        val entity = createTextCommandEntity(user, ticketId, comment)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = false, // TODO
            ticketId = ticketId,
            comment = comment,
            attachments = null,
            rating = null
        )
    }

    fun addRatingCommand(user: UserInternal, ticketId: Long, rating: Int): SyncRequest.Command.CreateComment {
        val entity = createRatingCommandEntity(user, ticketId, rating)
        addOrUpdatePendingCommand(entity)
        return SyncRequest.Command.CreateComment(
            localId = entity.localId,
            commandId = entity.commandId,
            userId = user.userId,
            appId = user.appId,
            creationTime = entity.creationTime,
            requestNewTicket = false, // TODO
            ticketId = ticketId,
            comment = null,
            attachments = null,
            rating = rating
        )
    }

    fun addReadCommand(user: UserInternal, ticketId: Long): SyncRequest.Command.MarkTicketAsRead {
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

    fun addAttachmentCommand(
        user: UserInternal,
        ticketId: Long,
        fileData: FileData,
    ): CommandEntity {
        val entity = createLocalAttachComment(user, ticketId, fileData)
        addOrUpdatePendingCommand(entity)
        return entity
    }

    fun createPushTokenCommand(
        user: UserInternal,
        token: String,
        tokenType: String,
    ) = SyncRequest.Command.SetPushToken(
        localId = getNextLocalId(),
        commandId = createCommandId(user),
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
        var commands = localCommandsStateFlow.value.toMutableList()

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

    fun addCommandError(commandError: CommandErrorEntity) {
        var errors = commandErrorsStateFlow.value.toMutableList()
        errors.let { list ->
            val existingIndex = list.indexOfFirst { it.commandId == commandError.commandId }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
            }
            list.add(commandError)
        }
        if (errors.size > MAX_PENDING_COMMENTS_SIZE) {
            errors = errors.subList(errors.size - MAX_PENDING_COMMENTS_SIZE, errors.size)
        }
        writeCommandErrors(errors)
    }


    /**
     * Provides all pending feed commands
     */
    private fun readPendingCommands(): List<CommandEntity> {
//        val rawJson = preferences.getString(PREFERENCE_KEY_TICKET_COMMANDS, "[]")
//        val commandsList = gson.fromJson<List<CommandEntity>>(rawJson, commandListTokenType).toMutableList()
//
//        if (commandsList.removeAll { localDataVerifier.isLocalCommandEmpty(it) }) {
//            writeCommands(commandsList)
//        }
//        return commandsList
        return emptyList()
    }

    private fun readCommandErrors(): List<CommandErrorEntity> {
        val rawJson = preferences.getString(PREFERENCE_KEY_TICKET_COMMAND_ERRORS, "[]")
//        val commandErrors = gson.fromJson<List<CommandErrorEntity>>(rawJson, commandListTokenType).toMutableList()
//
//        return commandErrors
        return emptyList()
    }

    fun getCommand(localId: Long): CommandEntity? {
        return localCommandsStateFlow.value.find { command -> command.localId == localId }
    }

    /**
     * Removes pending command from offline repository
     */
    fun removeCommand(commandId: String) {
        val commands = readPendingCommands().toMutableList()
        val removed = commands.removeAll { it.commandId == commandId }
        if (removed) {
            writeCommands(commands)
        }
    }

    fun removeCommandError(commandId: String) {

    }

    /**
     * Removes pending command from offline repository
     */
    fun removeCommand(command: CommandEntity) {
        val commands = readPendingCommands().toMutableList()
        val removed = commands.removeAll { it.commandId == command.commandId }
        if (removed) {
            writeCommands(commands)
        }
    }

    /**
     * Removes all commands from offline repository
     */
    fun removeAllCommands() {
        writeCommands(emptyList())
        writeCommandErrors(emptyList())
    }

    private fun createNewTicketCommandEntity(
        user: UserInternal,
        comment: String,
    ): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            localId = localId,
            commandId = createCommandId(user),
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
        )
    }

    private fun createTextCommandEntity(
        user: UserInternal,
        ticketId: Long,
        comment: String,
    ): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            localId = localId,
            commandId = createCommandId(user),
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = comment,
            attachments = null,
            ticketId = ticketId,
            rating = null,
            commentId = localId,
        )
    }

    private fun createRatingCommandEntity(
        user: UserInternal,
        ticketId: Long,
        rating: Int,
    ): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            localId = localId,
            commandId = createCommandId(user),
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = null,
            attachments = null,
            ticketId = ticketId,
            rating = rating,
            commentId = localId,
        )
    }

    private fun createLocalAttachComment(
        user: UserInternal,
        ticketId: Long,
        fileData: FileData,
    ): CommandEntity {
        val localId = getNextLocalId()
        return CommandEntity(
            localId = localId,
            commandId = createCommandId(user),
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = user.userId,
            appId = user.appId,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false, // TODO
            comment = null,
            attachments = listOf(createAttachment(fileData)),
            ticketId = ticketId,
            rating = null,
            commentId = localId
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
            localId = localId,
            commandId = createCommandId(user),
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
        )
    }

    private fun getNextLocalId(): Long {
        return lastLocalId.decrementAndGet()
    }

    private fun getNextAttachmentId(): Long {
        return lastAttachId.decrementAndGet()
    }

    private fun createCommandId(user: UserInternal): String {
        return "${System.currentTimeMillis()}_${user.userId}_${user.appId}"
    }

    private fun writeCommands(commands: List<CommandEntity>) {
//        val rawJson = gson.toJson(commands, commandListTokenType)
//        preferences.edit().putString(PREFERENCE_KEY_TICKET_COMMANDS, rawJson).apply()
//        localCommandsStateFlow.value = commands
    }

    private fun writeCommandErrors(errors: List<CommandErrorEntity>) {
//        val rawJson = gson.toJson(errors, commandErrorListTokenType)
//        preferences.edit().putString(PREFERENCE_KEY_TICKET_COMMAND_ERRORS, rawJson).apply()
//        commandErrorsStateFlow.value = errors
    }

    private companion object{
        const val PREFERENCE_KEY_OFFLINE_COMMENTS = "PREFERENCE_KEY_OFFLINE_COMMENTS"
        const val PREFERENCE_KEY_TICKET_COMMANDS = "PREFERENCE_KEY_TICKET_COMMANDS"
        const val PREFERENCE_KEY_TICKET_COMMAND_ERRORS = "PREFERENCE_KEY_TICKET_COMMAND_ERRORS"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commandListTokenType: Type = object : TypeToken<List<CommandEntity>>(){}.type
        val commandErrorListTokenType: Type = object : TypeToken<List<CommandErrorEntity>>(){}.type
    }
}