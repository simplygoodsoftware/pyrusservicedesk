package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class AddCommentRequest(
        repository: Repository,
        val ticketId: Int,
        val comment: Comment,
        val uploadFileHooks: UploadFileHooks? = null)
    : RequestBase<Int>(repository) {

    override suspend fun run(repository: Repository): ResponseBase<Int> {
        return repository.addComment(ticketId, comment, uploadFileHooks)
    }

}