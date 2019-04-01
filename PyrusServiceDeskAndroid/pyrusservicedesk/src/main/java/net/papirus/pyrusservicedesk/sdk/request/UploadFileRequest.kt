package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Request for uploading files.
 *
 * @param fileUploadRequestData data for making multipart request body.
 * @param uploadFileHooks hooks for cancellation and exposing upload progress.
 */
internal class UploadFileRequest(
        val fileUploadRequestData: FileUploadRequestData,
        val uploadFileHooks: UploadFileHooks?)
