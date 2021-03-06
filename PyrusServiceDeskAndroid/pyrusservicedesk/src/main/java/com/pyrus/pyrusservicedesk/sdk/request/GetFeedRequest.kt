package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.Response

/**
 * Request for obtaining ticket feed.
 * [requestsRemoteComments] should be TRUE to obtain remote comments
 * [keepUnread] True - keep comments unread. False - otherwise.
 */
internal class GetFeedRequest(
    repository: GeneralRepository,
    private val requestsRemoteComments: Boolean,
    private val keepUnread: Boolean
)
    : RequestBase<Comments>(repository) {

    override suspend fun run(repository: GeneralRepository): Response<Comments> =
        when {
            requestsRemoteComments -> repository.getFeed(keepUnread)
            else -> repository.getPendingFeedComments()
        }
}