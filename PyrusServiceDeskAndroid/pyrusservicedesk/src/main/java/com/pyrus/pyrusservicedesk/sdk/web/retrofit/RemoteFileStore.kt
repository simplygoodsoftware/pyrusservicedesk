package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.sync.FailDelay
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.request_body.ProgressRequestBody
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MultipartBody
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

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

    private val failDelay = FailDelay()

    private val filesQueue: ConcurrentLinkedDeque<UploadRequest> = ConcurrentLinkedDeque()
    private val isUploading = AtomicBoolean(false)

    suspend fun uploadFile(
        file: File,
        cancelHook: UploadFileHook,
        progressListener: (Int) -> Unit,
    ): Try<FileUploadResponseData> = suspendCancellableCoroutine {
        val request = UploadRequest(file, cancelHook, progressListener, it)

        if (cancelHook.isCancelled) {
            PLog.d(TAG, "uploadFile: ${file.name} has already been cancelled, nothing to upload")
            request.resume(cancelledTry())
            return@suspendCancellableCoroutine
        }

        it.invokeOnCancellation { filesQueue.remove(request) }

        filesQueue.add(request)
        PLog.d(TAG, "uploadFile: ${file.name} is added to the queue, bytes: ${file.length()}, " +
            "queue size: ${filesQueue.size}, isUploading: ${isUploading.get()}")

        tryStartUpload()
    }

    fun cancelUploading(cancelHook: UploadFileHook) {
        var removedCount = 0
        val iterator = filesQueue.iterator()
        while (iterator.hasNext()) {
            val request = iterator.next()
            if (request.cancelHook !== cancelHook) continue
            iterator.remove()
            request.resume(cancelledTry())
            removedCount++
        }

        PLog.d(TAG, "cancelUploading: removed from the queue: $removedCount, " +
            "queue size: ${filesQueue.size}, isUploading: ${isUploading.get()}")

        failDelay.cancel()
    }

    private fun tryStartUpload() {
        if (!isUploading.getAndSet(true)) {
            PLog.d(TAG, "tryStartUpload: uploading is started, queue size: ${filesQueue.size}")
            launch {
                startUpload()
            }
        }
        else {
            PLog.d(TAG, "tryStartUpload: uploading is already in progress, queue size: ${filesQueue.size}")
        }
    }

    private suspend fun startUpload() {
        try {
            var request = filesQueue.pollFirst()
            while (request != null) {
                try {
                    uploadFileInternal(request)
                }
                catch (t: Throwable) {
                    PLog.e(TAG, "startUpload: unexpected error on ${request.file.name}: $t")
                    request.resume(Try.Failure(t))
                    throw t
                }
                request = filesQueue.pollFirst()
            }
        }
        finally {
            isUploading.set(false)
            PLog.d(TAG, "startUpload: the queue is drained, isUploading: false")
            if (filesQueue.isNotEmpty()) {
                PLog.d(TAG, "startUpload: a new file has appeared in the queue, restart uploading")
                tryStartUpload()
            }
        }
    }

    private suspend fun uploadFileInternal(request: UploadRequest) {
        val fileName = request.file.name
        var attempt = 0

        while (!request.cancelHook.isCancelled) {
            attempt++
            PLog.d(TAG, "upload $fileName: attempt $attempt is started")
            val uploadTry = uploadFileInternal(request.file, request.cancelHook, request.progressListener)

            if (uploadTry is Try.Success) {
                PLog.d(TAG, "upload $fileName: attempt $attempt is succeeded, guid: ${uploadTry.value.guid}")
                failDelay.cancel()
                failDelay.clear()
                request.resume(uploadTry)
                return
            }

            val error = (uploadTry as Try.Failure).error
            if (request.cancelHook.isCancelled) {
                PLog.d(TAG, "upload $fileName: attempt $attempt is cancelled by the user, no retry")
                break
            }

            PLog.e(TAG, "upload $fileName: attempt $attempt is failed: $error, retry after delay")
            failDelay.cancelableDelay()
        }

        PLog.d(TAG, "upload $fileName: uploading is cancelled after $attempt attempt(s)")
        request.resume(cancelledTry())
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

    private fun cancelledTry(): Try<FileUploadResponseData> = Try.Failure(UploadCancelledException())

    private class UploadRequest(
        val file: File,
        val cancelHook: UploadFileHook,
        val progressListener: (Int) -> Unit,
        private val continuation: CancellableContinuation<Try<FileUploadResponseData>>,
    ) {

        private val isResumed = AtomicBoolean(false)

        fun resume(result: Try<FileUploadResponseData>) {
            if (isResumed.getAndSet(true)) return
            continuation.resume(result)
        }
    }

    companion object {
        const val TAG = "RemoteFileStore"
    }

}