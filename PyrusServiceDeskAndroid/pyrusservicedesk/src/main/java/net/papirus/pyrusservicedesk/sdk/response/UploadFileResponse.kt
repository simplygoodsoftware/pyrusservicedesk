package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData

/**
 * Response on [UploadFileRequest]
 */
internal class UploadFileResponse(
    error: ResponseError? = null,
    uploadData: FileUploadResponseData? = null)
    : ResponseBase<FileUploadResponseData>(error, uploadData)

