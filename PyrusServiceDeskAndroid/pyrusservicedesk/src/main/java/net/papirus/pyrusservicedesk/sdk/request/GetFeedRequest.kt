package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

/**
 * Request for obtaining ticket feed.
 */
internal class GetFeedRequest(repository: GeneralRepository): RequestBase<List<Comment>>(repository) {
    override suspend fun run(repository: GeneralRepository): ResponseBase<List<Comment>> = repository.getFeed()
}