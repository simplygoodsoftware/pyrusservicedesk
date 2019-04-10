package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseBase
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Request for adding comment to the ticket.
 *
 * @param ticketId id of ticket to add comment to.
 * @param comment comment which is should be added.
 * @param uploadFileHooks hooks for handling file uploading/downloading progress and cancellation.
 */
internal class AddCommentRequest(repository: GeneralRepository,
                                 val ticketId: Int,
                                 val comment: Comment,
                                 val uploadFileHooks: UploadFileHooks? = null)
    : RequestBase<Int>(repository) {

    override suspend fun run(repository: GeneralRepository): ResponseBase<Int> {
        return repository.addComment(ticketId, comment, uploadFileHooks)
    }
}