package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.Response
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl

/**
 * Request for removing all pending comments
 */
internal class RemoveAllPendingCommentsRequest(repository: GeneralRepository) : RequestBase<Boolean>(repository) {

    override suspend fun run(repository: GeneralRepository): Response<Boolean> {
        repository.removeAllPendingComments()
        return ResponseImpl.success(true)
    }
}
