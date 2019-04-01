package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Request for adding comment to ticket feed.
 *
 * @param comment comment which is should be added.
 * @param uploadFileHooks hooks for handling file uploading/downloading progress and cancellation.
 */
internal class AddFeedCommentRequest(repository: GeneralRepository,
                                     val comment: Comment,
                                     val uploadFileHooks: UploadFileHooks? = null)
    : RequestBase<Int>(repository) {

    override suspend fun run(repository: GeneralRepository): ResponseBase<Int> {
        return repository.addFeedComment(comment, uploadFileHooks)
    }

}