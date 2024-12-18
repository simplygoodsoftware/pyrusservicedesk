package com.pyrus.pyrusservicedesk.sdk

import android.content.ContentResolver
import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import com.pyrus.pyrusservicedesk.sdk.verify.LocalFileVerifier
import java.io.File
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
internal class FileResolver(private val contentResolver: ContentResolver) : LocalFileVerifier {

    /**
     * Provides data for making upload file request.
     * NULL may be returned if file form the specified [fileUri] was not found or [Uri.getScheme] != "content"
     */
    fun getUploadFileData(fileUri: Uri): FileUploadRequestData? {
        if (fileUri.scheme != ContentResolver.SCHEME_FILE) {
            return null
        }
        return FileResolverSchemeFile.getUploadFileData(fileUri)
    }

    fun getInputStream(fileUri: Uri): InputStream? {
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
    fun getFileData(fileUri: Uri?): FileData? {
        if (fileUri == null) {
            return null
        }
        if (fileUri.scheme != ContentResolver.SCHEME_FILE) {
            return null
        }
        return FileResolverSchemeFile.getFileData(fileUri)
    }

    /**
     * Helper for working with files.
     */
    private object FileResolverSchemeFile {

        /**
         * Provides data for making upload file request.
         * NULL may be returned if file form the specified [fileUri] was not found.
         */
        fun getUploadFileData(fileUri: Uri): FileUploadRequestData? {
            val file = File(fileUri.path ?: return null)
            if (file.exists().not())
                return null
            return FileUploadRequestData(file.name, file.inputStream())
        }

        fun getInputStream(fileUri: Uri): InputStream? {
            val file = File(fileUri.path ?: return null)
            if (file.exists().not()) {
                return null
            }
            return file.inputStream()
        }

        fun isLocalFileExists(localFileUri: Uri?): Boolean {
            if (localFileUri == null)
                return false
            val file = File(localFileUri.path ?: return false)
            return file.exists()
        }

        /**
         * Provides file data for the UI purposes. See [FileData].
         * NULL may be returned when [fileUri] is null.
         */
        fun getFileData(fileUri: Uri?): FileData? {
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
}
