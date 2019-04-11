package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseBase

/**
 * Request for obtaining ticket feed.
 */
internal class GetFeedRequest(repository: GeneralRepository): RequestBase<List<Comment>>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseBase<List<Comment>> = repository.getFeed()
}