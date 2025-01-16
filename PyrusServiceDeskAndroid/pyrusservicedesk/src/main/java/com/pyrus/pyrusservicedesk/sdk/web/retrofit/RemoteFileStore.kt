package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.sync.FailDelayCounter
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.request_body.ProgressRequestBody
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import okhttp3.MultipartBody
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class RemoteFileStore(
    private val api: ServiceDeskApi,
) : CoroutineScope {

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    override val coroutineContext: CoroutineContext = newSingleThreadContext(TAG) +
        SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            PLog.e(TAG, "upload file global error: ${throwable.message}")
            throwable.printStackTrace()
        }

    private val failDelayCounter = FailDelayCounter()

    private val filesQueue: ConcurrentLinkedDeque<UploadRequest> = ConcurrentLinkedDeque()
    private val isUploading = AtomicBoolean(false)

    suspend fun uploadFile(
        file: File,
        cancelHook: UploadFileHook,
        progressListener: (Int) -> Unit,
    ): Try<FileUploadResponseData> = suspendCoroutine {
        val request = UploadRequest(file, cancelHook, progressListener, it)
        filesQueue.add(request)

        tryStartUpload()
    }

    private fun tryStartUpload() {
        if (!isUploading.getAndSet(true)) {
            launch {
                val request = filesQueue.pollFirst()
                if (request != null) {
                    uploadFileInternal(request)
                }
                isUploading.set(false)
            }
        }
    }

    private suspend fun uploadFileInternal(request: UploadRequest) {
        val uploadTry = uploadFileInternal(request.file, request.cancelHook, request.progressListener)
        when (uploadTry) {
            is Try.Success -> {
                failDelayCounter.clear()
                request.continuation.resume(uploadTry)
            }
            is Try.Failure -> {
                val delay = failDelayCounter.getNextDelay()
                delay(delay)
                uploadFileInternal(request)
            }
        }
    }

    private suspend fun uploadFileInternal(
        file: File,
        cancelHook: UploadFileHook,
        progressListener: (Int) -> Unit,
    ): Try<FileUploadResponseData> {
        val requestBody = ProgressRequestBody(file, cancelHook, progressListener)

        val filePart = MultipartBody.Part.createFormData(
            "File",
            file.name.replace(Regex("[^\\p{ASCII}]"), "_"), // Only ASCII symbols are allowed
            requestBody
        )

        return api.uploadFile(filePart)
    }

    private data class UploadRequest(
        val file: File,
        val cancelHook: UploadFileHook,
        val progressListener: (Int) -> Unit,
        val continuation: Continuation<Try<FileUploadResponseData>>
    )

    companion object {
        const val TAG = "RemoteFileStore"
    }

}