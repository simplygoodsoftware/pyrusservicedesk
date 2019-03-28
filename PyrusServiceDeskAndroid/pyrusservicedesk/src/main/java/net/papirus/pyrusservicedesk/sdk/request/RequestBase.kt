package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.EmptyDataError
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback

internal abstract class RequestBase<ResponseData>(private val repository: GeneralRepository) {

    protected abstract suspend fun run(repository: GeneralRepository): ResponseBase<ResponseData>

    suspend fun execute(callback: ResponseCallback<ResponseData>){
        with(run(repository)) {
            when {
                error != null -> callback.onFailure(error)
                result == null -> callback.onFailure(EmptyDataError())
                else -> callback.onSuccess(result)
            }
        }
    }
}