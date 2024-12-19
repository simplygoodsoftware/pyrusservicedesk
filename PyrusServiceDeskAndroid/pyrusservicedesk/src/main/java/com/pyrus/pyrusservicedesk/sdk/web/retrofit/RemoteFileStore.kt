package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.request_body.ProgressRequestBody
import okhttp3.MultipartBody
import java.io.File

internal class RemoteFileStore(
    private val api: ServiceDeskApi,
) {

    suspend fun uploadFile(file: File, cancelHook: UploadFileHook, progressListener: (Int) -> Unit): Try<FileUploadResponseData> {

        val requestBody = ProgressRequestBody(file, cancelHook, progressListener)

        val filePart = MultipartBody.Part.createFormData(
            "File",
            file.name.replace(Regex("[^\\p{ASCII}]"), "_"), // Only ASCII symbols are allowed
            requestBody
        )

        return api.uploadFile(filePart)
    }

}