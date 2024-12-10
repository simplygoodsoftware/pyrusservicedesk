package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.getOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

/**
 * [SharedPreferences] based offline repository
 */
internal class LocalStore(
    private val preferences: SharedPreferences,
    private val localDataVerifier: LocalDataVerifier,
    private val gson: Gson,
) {

    private val localCommentsStateFlow = MutableStateFlow(getPendingFeedComments())

    fun commentsFlow(): Flow<List<Comment>> = localCommentsStateFlow

    /**
     * Adds pending feed comment
     */
    fun addPendingFeedComment(comment: Comment) {
        var comments = getPendingFeedComments().toMutableList()
        comments.let { list ->
            val existingIndex = list.indexOfFirst { it.localId == comment.localId }
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
     * Removes pending comment from offline repository
     */
    fun removePendingComment(comment: Comment) {
        val comments = getPendingFeedComments().toMutableList()
        val removed = comments.removeAll { it.localId == comment.localId }
        if (removed) {
            writeComments(comments)
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

    private companion object{
        const val PREFERENCE_KEY_OFFLINE_COMMENTS = "PREFERENCE_KEY_OFFLINE_COMMENTS"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commentListTokenType: Type = object : TypeToken<List<Comment>>(){}.type
    }
}