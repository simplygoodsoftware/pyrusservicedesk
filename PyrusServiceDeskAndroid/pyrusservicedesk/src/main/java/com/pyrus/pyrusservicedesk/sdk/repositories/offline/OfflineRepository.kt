package com.pyrus.pyrusservicedesk.sdk.repositories.offline

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.response.Response

/**
 * Repository for offline purposes
 */
internal interface OfflineRepository {

    /**
     * Adds pending feed comment
     */
    suspend fun addPendingFeedComment(comment: Comment): Response<Boolean>

    /**
     * Provides all pending feed comments
     */
    suspend fun getPendingFeedComments(): Response<List<Comment>>

    /**
     * Removes pending comment from offline repository
     */
    suspend fun removePendingComment(comment: Comment): Response<Boolean>
}