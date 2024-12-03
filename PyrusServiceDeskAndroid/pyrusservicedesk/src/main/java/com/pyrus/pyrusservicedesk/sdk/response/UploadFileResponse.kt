package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData

/**
 * Response on [UploadFileRequest]
 */
internal class UploadFileResponse(
    error: ResponseError? = null,
    uploadData: FileUploadResponseData? = null,
) : ResponseImpl<FileUploadResponseData>(error, uploadData)

