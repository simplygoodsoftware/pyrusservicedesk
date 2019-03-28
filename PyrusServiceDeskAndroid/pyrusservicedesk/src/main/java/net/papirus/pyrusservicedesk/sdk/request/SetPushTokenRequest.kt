package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase

internal class SetPushTokenRequest(repository: GeneralRepository,
                                   private val token: String) :
    RequestBase<Unit>(repository) {

    override suspend fun run(repository: GeneralRepository): ResponseBase<Unit> {
        return repository.setPushToken(token)
    }

}
