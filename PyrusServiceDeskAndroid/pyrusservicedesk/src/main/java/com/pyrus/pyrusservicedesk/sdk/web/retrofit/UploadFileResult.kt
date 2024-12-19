package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData

internal sealed interface UploadFileResult {

    data class Progress(val progress: Int) : UploadFileResult

    data class Done(val result: Try<FileUploadResponseData>): UploadFileResult

}