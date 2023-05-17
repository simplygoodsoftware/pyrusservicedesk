package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl

/**
 * Request for registering push token.
 * @param token if null push notifications stop.
 * @param tokenType cloud messaging type.
 */
internal class SetPushTokenRequest(
    repository: GeneralRepository,
    private val token: String?,
    private val tokenType: String,
) : RequestBase<Unit>(repository) {

    override suspend fun run(repository: GeneralRepository): ResponseImpl<Unit> {
        return repository.setPushToken(token, tokenType)
    }

}
