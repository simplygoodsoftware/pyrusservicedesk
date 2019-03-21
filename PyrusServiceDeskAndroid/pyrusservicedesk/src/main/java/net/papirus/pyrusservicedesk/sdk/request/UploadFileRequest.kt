package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class UploadFileRequest(
        val fileUploadRequestData: FileUploadRequestData,
        val uploadFileHooks: UploadFileHooks?)
