package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.Response
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
    : RequestBase<AddCommentResponseData>(repository) {

    override suspend fun run(repository: GeneralRepository): Response<AddCommentResponseData> {
        return repository.addComment(ticketId, comment, uploadFileHooks)
    }
}