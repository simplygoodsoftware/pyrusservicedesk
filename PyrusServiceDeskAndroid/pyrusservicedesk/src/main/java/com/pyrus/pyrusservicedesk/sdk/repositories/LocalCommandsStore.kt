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

    private val localCommentsStateFlow = MutableStateFlow(getPendingFeedComments())
    private val localCommandsStateFlow = MutableStateFlow(getPendingFeedCommands())
    private var lastTicketId = MutableStateFlow(-1)

    fun commentsFlow(): Flow<List<Comment>> = localCommentsStateFlow

//    val commandsFlow(): Flow<List<Command>> = localCommandsStateFlow

    fun getUpdatedLastTicketId() = --lastTicketId.value

    fun getLastTicketId() = lastTicketId.value

    /**
     * Adds pending feed comment
     */
    fun addPendingFeedComment(comment: Comment) {
        var comments = localCommentsStateFlow.value.toMutableList()

        comments.let { list ->
            val existingIndex = list.indexOfFirst { it.id == comment.id }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
            }
            list.add(comment)
        }
        if (comments.size > MAX_PENDING_COMMENTS_SIZE) {
            comments = comments.subList(comments.size - MAX_PENDING_COMMENTS_SIZE, comments.size)
        }
        writeComments(comments)
    }

    /**
     * Adds pending feed comment
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
     * Provides all pending feed comments
     */
    fun getPendingFeedComments(): List<Comment> {
        val rawJson = preferences.getString(PREFERENCE_KEY_OFFLINE_COMMENTS, "[]")
        val commentsList = gson.fromJson<List<Comment>>(rawJson, commentListTokenType).toMutableList()

        if (commentsList.removeAll { localDataVerifier.isLocalCommentEmpty(it) }) {
            writeComments(commentsList)
        }
        return commentsList
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

    fun getComment(id: Long): Comment? {
        return getPendingFeedComments().find { comment -> comment.id == id }
    }

    fun getCommand(id: Long): TicketCommandDto? {
        return getPendingFeedCommands().find { command -> getCommentId(command.commandId) == id }
    }

    private fun getCommentId(uuid: String): Long {
        return uuid.substringAfter("commentId=").substringBefore(";").toLong()
    }

    /**
     * Removes pending comment from offline repository
     */
    fun removePendingComment(comment: Comment) {
        val comments = getPendingFeedComments().toMutableList()
        val removed = comments.removeAll { it.id == comment.id }
        if (removed) {
            writeComments(comments)
        }
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
     * Removes all pending comments from offline repository
     */
    fun removeAllPendingComments() {
        writeComments(emptyList())
    }

    private fun writeComments(comments: List<Comment>) {
        val rawJson = gson.toJson(comments, commentListTokenType)
        preferences.edit().putString(PREFERENCE_KEY_OFFLINE_COMMENTS, rawJson).apply()
        localCommentsStateFlow.value = comments
    }

    private fun writeCommands(commands: List<TicketCommandDto>) {
        val rawJson = gson.toJson(commands, commandListTokenType)
        preferences.edit().putString(PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS, rawJson).apply()
        localCommandsStateFlow .value = commands
    }

    private companion object{
        const val PREFERENCE_KEY_OFFLINE_COMMENTS = "PREFERENCE_KEY_OFFLINE_COMMENTS"
        const val PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS = "PREFERENCE_KEY_OFFLINE_TICKET_COMMANDS"
        const val PREFERENCE_KEY_OFFLINE = "PREFERENCE_KEY_OFFLINE"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commentListTokenType: Type = object : TypeToken<List<Comment>>(){}.type
        val commandListTokenType: Type = object : TypeToken<List<TicketCommandDto>>(){}.type
    }
}