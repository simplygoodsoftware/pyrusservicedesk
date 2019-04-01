package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.EmptyDataError
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback

/**
 * Base request class. Works directly with [repository]. [ResponseCallback] contains pure data entities.
 *
 * @param repository repository that is used for executing requests
 */
internal abstract class RequestBase<ResponseData>(private val repository: GeneralRepository) {

    /**
     * Implementation should provide response using the given [repository]
     *
     * @return response that contains either data or error.
     */
    protected abstract suspend fun run(repository: GeneralRepository): ResponseBase<ResponseData>

    /**
     * Launches request. Result of launching request is deferred delivered to [callback].
     */
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