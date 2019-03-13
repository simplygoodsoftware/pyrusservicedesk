package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body

import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.internal.Util
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.InputStream

internal class UploadFileRequestBody(
        private val fileName: String,
        private val fileStream: InputStream)
    :RequestBodyBase("", "") {

    fun toMultipartBody(): MultipartBody.Part {
        val requestFileBody = object: RequestBody(){
            override fun contentType(): MediaType? {
                return MediaType.parse("multipart/form-responseData")
            }

            override fun contentLength(): Long {
                return fileStream.available().toLong()
            }

            override fun writeTo(sink: BufferedSink) {
                var source: Source? = null
                try {
                    source = Okio.source(fileStream)
                    sink.writeAll(source!!)
                } finally {
                    Util.closeQuietly(source)
                }
            }

        }
        return MultipartBody.Part.createFormData("File", fileName, requestFileBody)
    }
}