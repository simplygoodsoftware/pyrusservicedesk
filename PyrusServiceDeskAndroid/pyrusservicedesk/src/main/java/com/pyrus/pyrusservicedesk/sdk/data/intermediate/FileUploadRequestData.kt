package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import java.io.InputStream

/**
 * Intermediate data for uploading files to server.
 * NB: [fileInputStream] should be closed properly when it is not needed anymore.
 */
internal class FileUploadRequestData(
    val fileName: String,
    val fileInputStream: InputStream)