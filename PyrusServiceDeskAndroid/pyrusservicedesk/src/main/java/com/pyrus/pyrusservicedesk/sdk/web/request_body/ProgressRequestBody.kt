package com.pyrus.pyrusservicedesk.sdk.web.request_body

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ProgressRequestBody(
    private val file: File,
    private val progressListener: (Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? {
        return MediaType.parse(MEDIA_TYPE)
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return file.length()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(BUFFER_SIZE)
        var uploaded: Long = 0
        FileInputStream(file).use { stream ->
            var read: Int
            while ((stream.read(buffer).also { read = it }) != -1) {
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                val progress = (uploaded.toDouble()/fileLength * 100).toInt()
                progressListener(progress)
            }
        }
    }

    private companion object {
        const val MEDIA_TYPE = "multipart/form-responseData"
        private const val BUFFER_SIZE = 1024
    }
}