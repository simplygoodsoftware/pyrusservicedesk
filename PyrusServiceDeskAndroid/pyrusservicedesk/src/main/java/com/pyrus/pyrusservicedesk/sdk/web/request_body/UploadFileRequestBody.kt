package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import kotlinx.coroutines.isActive
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

/**
 * Request body for sending single file to the server.
 *
 * @param fileName name of the file to be sent.
 * @param fileStream file stream to read data from for sending.
 * @param uploadFileHooks hooks for publishing progress and for checking cancellation signal.
 * @param context current coroutine context. Used for checking if it is alive while file uploading.
 */
internal class UploadFileRequestBody(
        private val fileName: String,
        private val fileStream: InputStream,
        private val uploadFileHooks: UploadFileHooks?,
        private val context: CoroutineContext) {

    private companion object {
        const val MEDIA_TYPE = "multipart/form-responseData"
    }

    private val fileSize = fileStream.available().toLong()

    init {
        uploadFileHooks?.onProgressPercentChanged(0)
    }

    /**
     * Prepares multipart body.
     */
    fun toMultipartBody(): MultipartBody.Part {
        val requestFileBody = object: RequestBody(){
            override fun contentType(): MediaType? {
                return MEDIA_TYPE.toMediaTypeOrNull()
            }

            override fun contentLength(): Long {
                return fileStream.available().toLong()
            }

            override fun writeTo(sink: BufferedSink) {
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var uploaded = 0L
                fileStream.use { fileStream ->
                    var read: Int = fileStream.read(buffer)
                    while (read != -1) {
                        if (uploadFileHooks?.isCancelled == true)
                            break
                        if (!context.isActive) {
                            throw IOException("Scope is no longer active")
                        }
                        uploaded += read
                        sink.write(buffer, 0, read)
                        uploadFileHooks?.onProgressPercentChanged(uploaded.toProgress())
                        read = fileStream.read(buffer)
                    }
                }
            }

        }
        return MultipartBody.Part.createFormData(
            "File",
            fileName.replace(Regex("[^\\p{ASCII}]"), "_"), // Only ASCII symbols are allowed
            requestFileBody)
    }

    private fun Long.toProgress(): Int {
        return (toDouble()/fileSize * 100).toInt()
    }
}