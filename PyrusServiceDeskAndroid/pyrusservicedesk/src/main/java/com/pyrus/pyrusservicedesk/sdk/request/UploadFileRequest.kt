package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook

/**
 * Request for uploading files.
 *
 * @param fileUploadRequestData data for making multipart request body.
 * @param uploadFileHook hooks for cancellation and exposing upload progress.
 */
internal class UploadFileRequest(
    val fileUploadRequestData: FileUploadRequestData,
    val uploadFileHook: UploadFileHook?,
)
