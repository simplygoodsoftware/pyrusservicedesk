package com.pyrus.pyrusservicedesk.sdk

import android.net.Uri
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import com.pyrus.pyrusservicedesk.utils.RequestUtils
import java.io.File

/**
 * Helper for working with files.
 */
internal object FileResolverSchemeFile : FileResolver {

    /**
     * Provides data for making upload file request.
     * NULL may be returned if file form the specified [fileUri] was not found.
     */
    override fun getUploadFileData(fileUri: Uri): FileUploadRequestData? {
        val file = File(fileUri.path ?: return null)
        if (file.exists().not())
            return null
        return FileUploadRequestData(file.name, file.inputStream())
    }

    override fun isLocalFileExists(localFileUri: Uri?): Boolean {
        if (localFileUri == null)
            return false
        val file = File(localFileUri.path ?: return false)
        return file.exists()
    }

    /**
     * Provides file data for the UI purposes. See [FileData].
     * NULL may be returned when [fileUri] is null.
     */
    override fun getFileData(fileUri: Uri?): FileData? {
        if (fileUri == null)
            return null
        val file = File(fileUri.path ?: return null)
        val size = file.length()
        if (file.exists().not() || size > RequestUtils.MAX_FILE_SIZE_BYTES)
            return null
        return FileData(
            file.name,
            size.toInt(),
            fileUri,
            true)
    }

}
