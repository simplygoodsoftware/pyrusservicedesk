package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Type

/**
 * [SharedPreferences] based offline repository
 */
internal class LocalCommandsStore(
    private val preferences: SharedPreferences,
    private val localDataVerifier: LocalDataVerifier,
    private val gson: Gson,
) {

    private val localCommandsStateFlow = MutableStateFlow(getPendingFeedCommands())
    private var lastTicketId = MutableStateFlow(-1)

    val commandsFlow: Flow<List<TicketCommandDto>> = localCommandsStateFlow

    fun getUpdatedLastTicketId() = --lastTicketId.value

    fun getLastTicketId() = lastTicketId.value

    /**
     * Adds command to store
     */
    fun addPendingFeedCommand(command: TicketCommandDto) {
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

    /**
     * Provides all pending feed commands
     */
    fun getPendingFeedCommands(): List<TicketCommandDto> {
        val rawJson = preferences.getString(PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS, "[]")
        //val commandsList = gson.fromJson<List<TicketCommandDto>>(rawJson, commandListTokenType).toMutableList()

//        if (commandsList.removeAll { localDataVerifier.isLocalCommandEmpty(it) }) {
//            writeCommands(commandsList)
//        }
        return emptyList()//commandsList TODO
    }

    fun getCommand(id: Long): TicketCommandDto? {
        return getPendingFeedCommands().find { command -> getCommentId(command.commandId) == id }
    }

    private fun getCommentId(uuid: String): Long {
        return uuid.substringAfter("commentId=").substringBefore(";").toLong()
    }

    /**
     * Removes pending command from offline repository
     */
    fun removePendingCommand(command: TicketCommandDto) {
        val commands = getPendingFeedCommands().toMutableList()
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
    }

    private fun writeCommands(commands: List<TicketCommandDto>) {
        val rawJson = gson.toJson(commands, commandListTokenType)
        preferences.edit().putString(PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS, rawJson).apply()
        localCommandsStateFlow .value = commands
    }

    private companion object{
        const val PREFERENCE_KEY_OFFLINE_COMMENTS = "PREFERENCE_KEY_OFFLINE_COMMENTS"
        const val PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS = "PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commentListTokenType: Type = object : TypeToken<List<Comment>>(){}.type
        val commandListTokenType: Type = object : TypeToken<List<TicketCommandDto>>(){}.type
    }
}