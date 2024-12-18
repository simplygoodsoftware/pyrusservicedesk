package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.web.request_body.UploadFileRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.coroutines.coroutineContext

internal class FileRemoteStore(
    private val api: ServiceDeskApi,
    private val fileResolver: FileResolver,
//    private val fileManager: FileManager,
) {

    private val sendingAttachment = MutableStateFlow<List<Uri>>(emptyList())


    suspend fun uploadFile(file: File): Try<FileUploadResponseData> {
        TODO()
//        val uploadFileTry = api.uploadFile(
//            UploadFileRequestBody(
//                request.fileUploadRequestData.fileName,
//                request.fileUploadRequestData.fileInputStream,
//                request.uploadFileHooks,
//                coroutineContext
//            ).toMultipartBody()
//        )
//
//        return uploadFileTry
    }

    fun cancelSending(uri: Uri) {

    }


}