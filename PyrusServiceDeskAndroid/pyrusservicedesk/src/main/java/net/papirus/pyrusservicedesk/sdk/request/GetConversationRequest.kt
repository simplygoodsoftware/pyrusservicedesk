package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

internal class GetConversationRequest(repository: Repository): RequestBase<List<Comment>>(repository) {
    override fun run(repository: Repository): ResponseBase<List<Comment>> = repository.getConversation()
}