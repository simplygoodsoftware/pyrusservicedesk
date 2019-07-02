package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.Response
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Request for adding comment to ticket feed.
 *
 * @param comment comment which is should be added.
 * @param uploadFileHooks hooks for handling file uploading/downloading progress and cancellation.
 */
internal class AddFeedCommentRequest(repository: GeneralRepository,
                                     val comment: Comment,
                                     val uploadFileHooks: UploadFileHooks? = null)
    : RequestBase<AddCommentResponseData>(repository) {

    override suspend fun run(repository: GeneralRepository): Response<AddCommentResponseData> {
        return repository.addFeedComment(comment, uploadFileHooks)
    }

}