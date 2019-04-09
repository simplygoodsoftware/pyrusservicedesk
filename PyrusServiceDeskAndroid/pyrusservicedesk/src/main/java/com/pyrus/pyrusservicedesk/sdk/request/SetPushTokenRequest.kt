package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseBase

/**
 * Request for registering push token.
 */
internal class SetPushTokenRequest(repository: GeneralRepository,
                                   private val token: String) :
    RequestBase<Unit>(repository) {

    override suspend fun run(repository: GeneralRepository): ResponseBase<Unit> {
        return repository.setPushToken(token)
    }

}
