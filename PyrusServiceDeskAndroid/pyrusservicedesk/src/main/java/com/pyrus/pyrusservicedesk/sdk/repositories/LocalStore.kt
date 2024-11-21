package com.pyrus.pyrusservicedesk.sdk.repositories

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.utils.Try
import com.pyrus.pyrusservicedesk.utils.getOrNull
import java.lang.reflect.Type

/**
 * [SharedPreferences] based offline repository
 */
internal class LocalStore(
    private val preferences: SharedPreferences,
    private val localDataVerifier: LocalDataVerifier,
    private val gson: Gson,
) {

    private companion object{
        const val PREFERENCE_KEY_OFFLINE_COMMENTS = "PREFERENCE_KEY_OFFLINE_COMMENTS"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commentListTokenType: Type = object : TypeToken<List<Comment>>(){}.type
    }

    /**
     * Adds pending feed comment
     */
    suspend fun addPendingFeedComment(comment: Comment): Try<Boolean> {
        var comments = getPendingFeedComments().getOrNull()?.comments?.toMutableList() ?: mutableListOf()
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
        return Try.Success(true)
    }

    /**
     * Provides all pending feed comments
     */
    suspend fun getPendingFeedComments(): Try<Comments> {
        val rawJson = preferences.getString(PREFERENCE_KEY_OFFLINE_COMMENTS, "[]")
        val commentsList = gson.fromJson<List<Comment>>(rawJson, commentListTokenType).toMutableList()

        if (commentsList.removeAll { localDataVerifier.isLocalCommentEmpty(it) }) {
            writeComments(commentsList)
        }
        return Try.Success(Comments(commentsList))
    }

    /**
     * Removes pending comment from offline repository
     */
    suspend fun removePendingComment(comment: Comment): Try<Boolean> {
        val comments = getPendingFeedComments().getOrNull()?.comments?.toMutableList()
        val removed = comments?.removeAll { it.localId == comment.localId } ?: false
        if (removed && comments != null) {
            writeComments(comments)
        }
        return Try.Success(removed)
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
    }
}