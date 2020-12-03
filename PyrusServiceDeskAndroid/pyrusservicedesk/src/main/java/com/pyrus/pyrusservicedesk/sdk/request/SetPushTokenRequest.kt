package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl

/**
 * Request for registering push token.
 * @param token if null push notifications stop.
 */
internal class SetPushTokenRequest(
    repository: GeneralRepository,
    private val token: String?
) : RequestBase<Unit>(repository) {

    override suspend fun run(repository: GeneralRepository): ResponseImpl<Unit> {
        return repository.setPushToken(token)
    }

}
