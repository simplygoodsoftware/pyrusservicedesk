package com.pyrus.pyrusservicedesk.sdk.repositories.offline

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.response.Response
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import java.lang.reflect.Type

/**
 * [SharedPreferences] based offline repository
 */
internal class PreferenceOfflineRepository(private val preferences: SharedPreferences,
                                           private val localDataVerifier: LocalDataVerifier,
                                           private val gson: Gson)
    : OfflineRepository {

    private companion object{
        const val PREFERENCE_KEY_OFFLINE_COMMENTS = "PREFERENCE_KEY_OFFLINE_COMMENTS"
        const val MAX_PENDING_COMMENTS_SIZE = 20
        val commentListTokenType: Type = object : TypeToken<List<Comment>>(){}.type
    }

    override suspend fun addPendingFeedComment(comment: Comment): Response<Boolean> {
        var comments = getPendingFeedComments().getData()?.toMutableList() ?: mutableListOf()
        comments.let { list ->
            val existingIndex = list.indexOfFirst { it.localId == comment.localId }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
            }
            list.add(comment)
            list
        }
        if (comments.size > MAX_PENDING_COMMENTS_SIZE) {
            comments = comments.subList(comments.size - MAX_PENDING_COMMENTS_SIZE, comments.size)
        }
        writeComments(comments)
        return ResponseImpl.success(true)
    }

    override suspend fun getPendingFeedComments(): Response<List<Comment>> {
        val commentsList =
            gson.fromJson<List<Comment>>(
                preferences.getString(PREFERENCE_KEY_OFFLINE_COMMENTS, "[]"),
                commentListTokenType)
            .toMutableList()
        if (commentsList.removeAll { localDataVerifier.isLocalCommentEmpty(it) })
            writeComments(commentsList)
        return ResponseImpl.success(commentsList)
    }

    override suspend fun removePendingComment(comment: Comment): Response<Boolean> {
        val removed =
            getPendingFeedComments()
                .getData()
                ?.toMutableList()
                ?.let { list ->
                    if (list.removeAll{ it.localId == comment.localId }) {
                        writeComments(list)
                        return@let true
                    }
                    false
                }
                ?: false
        return ResponseImpl.success(removed)
    }

    private fun writeComments(comments: List<Comment>) {
        preferences
            .edit()
            .putString(
                PREFERENCE_KEY_OFFLINE_COMMENTS,
                gson.toJson(comments, commentListTokenType))
            .apply()
    }
}