package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.Response

/**
 * Request for obtaining ticket feed.
 * [requestsRemoteComments] should be TRUE to obtain remote comments
 */
internal class GetFeedRequest(repository: GeneralRepository,
                              private val requestsRemoteComments: Boolean)
    : RequestBase<List<Comment>>(repository) {

    override suspend fun run(repository: GeneralRepository): Response<List<Comment>> =
        when {
            requestsRemoteComments -> repository.getFeed()
            else -> repository.getPendingFeedComments()
        }
}