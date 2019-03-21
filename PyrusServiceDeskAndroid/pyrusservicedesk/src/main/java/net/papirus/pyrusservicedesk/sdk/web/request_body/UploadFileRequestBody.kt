package net.papirus.pyrusservicedesk.sdk.web.request_body

import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

internal class UploadFileRequestBody(
        private val fileName: String,
        private val fileStream: InputStream,
        private val uploadFileHooks: UploadFileHooks?) {

    private val fileSize = fileStream.available().toLong()

    init {
        uploadFileHooks?.onProgressPercentChanged(0)
    }

    fun toMultipartBody(): MultipartBody.Part {
        val requestFileBody = object: RequestBody(){
            override fun contentType(): MediaType? {
                return MediaType.parse("multipart/form-responseData")
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
                        uploaded += read
                        sink.write(buffer, 0, read)
                        uploadFileHooks?.onProgressPercentChanged(calculateProgress(uploaded))
                        read = fileStream.read(buffer)
                    }
                }
            }

        }
        return MultipartBody.Part.createFormData("File", fileName, requestFileBody)
    }

    private fun calculateProgress(bytesUploaded: Long): Int {
        return (bytesUploaded.toDouble()/fileSize * 100).toInt()
    }
}