package net.papirus.pyrusservicedesk.presentation.usecase

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

internal abstract class UseCaseBase<T>(private val scope: CoroutineScope) {

    protected abstract suspend fun run(): UseCaseResult<T>

    fun execute(): LiveData<UseCaseResult<T>> {
        val result =
            MutableLiveData<UseCaseResult<T>>()
        scope.launch {
            val job = async { run() }
            withContext(Dispatchers.Main) {
                result.value = job.await()
            }
        }
        return result
    }
}

internal class UseCaseResult<T>(val data: T? = null, val error: ResponseError? = null){
    fun hasError() = error != null
}