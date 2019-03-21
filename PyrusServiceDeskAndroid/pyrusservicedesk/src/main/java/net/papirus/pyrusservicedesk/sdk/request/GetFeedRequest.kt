package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

internal class GetFeedRequest(repository: Repository): RequestBase<List<Comment>>(repository) {
    override suspend fun run(repository: Repository): ResponseBase<List<Comment>> = repository.getFeed()
}