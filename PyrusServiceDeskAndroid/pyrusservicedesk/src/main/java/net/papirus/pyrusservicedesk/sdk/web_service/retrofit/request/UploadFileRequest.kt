package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request

import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body.UploadFileRequestBody
import java.io.InputStream

internal class UploadFileRequest(
        val ticketId: Int,
        val fileName: String,
        private val fileStream: InputStream,
        private val uploadCallbacks: FileUploadCallbacks)
    : RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): UploadFileRequestBody {
        return UploadFileRequestBody(fileName, fileStream, uploadCallbacks)
    }

}
