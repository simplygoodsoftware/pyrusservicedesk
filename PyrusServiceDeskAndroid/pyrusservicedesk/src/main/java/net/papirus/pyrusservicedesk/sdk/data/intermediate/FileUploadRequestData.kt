package net.papirus.pyrusservicedesk.sdk.data.intermediate

import java.io.InputStream

internal class FileUploadRequestData(
    val fileName: String,
    val fileInputStream: InputStream)