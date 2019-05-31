package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.Response

/**
 * Request for removing pending comment
 */
internal class RemovePendingCommentRequest(repository: GeneralRepository,
                                           private val comment: Comment)
    : RequestBase<Boolean>(repository) {

    override suspend fun run(repository: GeneralRepository): Response<Boolean> {
        return repository.removePendingComment(comment)
    }
}
