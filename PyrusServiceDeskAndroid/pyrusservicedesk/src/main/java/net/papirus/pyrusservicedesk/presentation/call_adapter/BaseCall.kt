package net.papirus.pyrusservicedesk.presentation.call_adapter

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

/**
 * Base class that bounds requests to a data that can be consumed by ViewModels in a form of LiveData.
 * The reason on having this objects is that this can be easily extended to perform custom transformations of the
 * domain data provided by [run] implementation using the given [scope].
 */
internal abstract class BaseCall<T>(private val scope: CoroutineScope) {

    /**
     * Implementations can execute requests here and provide the taken result.
     */
    protected abstract suspend fun run(): CallResult<T>

    /**
     * Asynchronously executes the request and deferred delivers the result to the output LiveData.
     *
     * @return live data that deferred delivers result to its observer.
     */
    fun execute(): LiveData<CallResult<T>> {
        val result =
            MutableLiveData<CallResult<T>>()
        scope.launch {
            val job = async { run() }
            withContext(Dispatchers.Main) {
                result.value = job.await()
            }
        }
        return result
    }
}

/**
 * Result of [BaseCall.execute] method. Can either contain [data] of contain [error].
 */
internal class CallResult<T>(val data: T? = null, val error: ResponseError? = null){
    /**
     * @return true if result contains an error.
     */
    fun hasError() = error != null
}