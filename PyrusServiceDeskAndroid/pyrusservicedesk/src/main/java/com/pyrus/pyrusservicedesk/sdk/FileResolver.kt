package com.pyrus.pyrusservicedesk.sdk

import android.net.Uri
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import com.pyrus.pyrusservicedesk.sdk.verify.LocalFileVerifier
import java.io.InputStream
import java.io.OutputStream

/**
 * Helper for dealing with files
 */
internal interface FileResolver: LocalFileVerifier {

    /**
     * Provides data for making upload file request.
     * NULL may be returned if file form the specified [fileUri] was not found or [Uri.getScheme] != "content"
     */
    fun getUploadFileData(fileUri: Uri): FileUploadRequestData?

    fun getInputStream(fileUri: Uri): InputStream?

    /**
     * Provides file data for the UI purposes. See [FileData].
     * NULL may be returned when [fileUri] is null or [fileUri] has [Uri.getScheme] != "content"
     */
    fun getFileData(fileUri: Uri?): FileData?
}