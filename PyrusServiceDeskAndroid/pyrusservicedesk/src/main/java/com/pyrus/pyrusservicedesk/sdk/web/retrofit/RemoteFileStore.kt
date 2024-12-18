package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.web.request_body.UploadFileRequestBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File

internal class RemoteFileStore(
    private val api: ServiceDeskApi,
    private val fileResolver: FileResolver,
//    private val fileManager: FileManager,
) {

    private val sendingAttachment = MutableStateFlow<List<Uri>>(emptyList())


    suspend fun uploadFile(file: File): Flow<UploadResult> {
        TODO()
        api.uploadFile(file)

//        val uploadFileTry = api.uploadFile(
            UploadFileRequestBody(
                request.fileUploadRequestData.fileName,
                request.fileUploadRequestData.fileInputStream,
                request.uploadFileHooks,
                coroutineContext
            ).toMultipartBody()
//        )
//
//        return uploadFileTry
    }

    fun cancelSending(uri: Uri) {

    }

    sealed interface UploadResult {

        data class Progress(val progress: Int) : UploadResult

        data class Success(val response: FileUploadResponseData) : UploadResult

        data object Failed : UploadResult

    }


}