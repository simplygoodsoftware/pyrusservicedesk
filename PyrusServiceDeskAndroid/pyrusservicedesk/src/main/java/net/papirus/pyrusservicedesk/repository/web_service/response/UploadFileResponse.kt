package net.papirus.pyrusservicedesk.repository.web_service.response

import net.papirus.pyrusservicedesk.repository.data.intermediate.FileUploadData
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.UploadFileRequest

internal class UploadFileResponse(
        status: Status = Status.Ok,
        request: UploadFileRequest,
        uploadData: FileUploadData? = null)
    : ResponseBase<UploadFileRequest, FileUploadData>(status, request, uploadData)
