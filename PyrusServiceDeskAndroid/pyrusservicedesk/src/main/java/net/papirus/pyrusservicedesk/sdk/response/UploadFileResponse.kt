package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData

internal class UploadFileResponse(
    status: ResponseStatus = ResponseStatus.Ok,
    uploadData: FileUploadResponseData? = null)
    : ResponseBase1<FileUploadResponseData>(status, uploadData)

