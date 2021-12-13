package com.pyrus.pyrusservicedesk.sdk

import android.content.ContentResolver
import android.net.Uri
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import java.io.InputStream

/**
 * Helper for working with files.
 *
 * NB:
 * Only file URIs with schema == "content" can be processed.
 * In other cases NULL is returned when [getUploadFileData] or [getFileData] is invoked.
 *
 * @param contentResolver content resolver instance provided by an application
 */
internal class FileResolverImpl(private val contentResolver: ContentResolver) : FileResolver {

    /**
     * Provides data for making upload file request.
     * NULL may be returned if file form the specified [fileUri] was not found or [Uri.getScheme] != "content"
     */
    override fun getUploadFileData(fileUri: Uri): FileUploadRequestData? {
        if (fileUri.scheme == ContentResolver.SCHEME_FILE) {
            return FileResolverSchemeFile.getUploadFileData(fileUri)
        }

        return null
    }

    override fun getInputStream(fileUri: Uri): InputStream? {
        return when (fileUri.scheme) {
            ContentResolver.SCHEME_FILE -> FileResolverSchemeFile.getInputStream(fileUri)
            ContentResolver.SCHEME_CONTENT -> contentResolver.openInputStream(fileUri)
            else -> null
        }
    }

    override fun isLocalFileExists(localFileUri: Uri?): Boolean {
        if (localFileUri == null) {
            return false
        }
        if (localFileUri.scheme != ContentResolver.SCHEME_FILE) {
            return false
        }
        return FileResolverSchemeFile.isLocalFileExists(localFileUri)
    }

    /**
     * Provides file data for the UI purposes. See [FileData].
     * NULL may be returned when [fileUri] is null or [fileUri] has [Uri.getScheme] != "content"
     */
    override fun getFileData(fileUri: Uri?): FileData? {
        if (fileUri == null) {
            return null
        }
        if (fileUri.scheme != ContentResolver.SCHEME_FILE) {
            return null
        }
        return FileResolverSchemeFile.getFileData(fileUri)
    }
}
