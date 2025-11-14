package com.pyrus.pyrusservicedesk.sdk

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData
import com.pyrus.pyrusservicedesk.sdk.verify.LocalFileVerifier
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
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
internal class FileResolver(private val contentResolver: ContentResolver, private val context: Context) : LocalFileVerifier {

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
            ContentResolver.SCHEME_CONTENT -> {
                if (isGoogleDriveUri(fileUri)) {
                    getInputStreamForGoogleDrive(fileUri)
                        ?: contentResolver.openInputStream(fileUri)
                } else {
                    contentResolver.openInputStream(fileUri)
                }
            }

            else -> null
        }
    }

    private fun handleVirtualFile(uri: Uri): InputStream? {
        return try {
            tryVirtualFileViaDocumentFile(uri) ?:
            tryVirtualFileWithBuffering(uri) ?:
            createVirtualFileCopy(uri)
        } catch (e: Exception) {
            null
        }
    }

    private fun isGoogleDriveUri(uri: Uri): Boolean {
        return uri.authority == "com.google.android.apps.docs.storage" ||
            uri.toString().contains("google.android.apps.docs")
    }

    @SuppressLint("Range")
    private fun getInputStreamForGoogleDrive(uri: Uri): InputStream? {
        return try {
            contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            handleVirtualFile(uri)
        }
    }

    private fun tryVirtualFileViaDocumentFile(uri: Uri): InputStream? {
        return try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile != null && documentFile.exists()) {
                contentResolver.openInputStream(documentFile.uri)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryVirtualFileWithBuffering(uri: Uri): InputStream? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BufferedInputStream(inputStream).also { bufferedStream ->
                bufferedStream.mark(1)
                val firstByte = bufferedStream.read()
                if (firstByte != -1) {
                    bufferedStream.reset()
                } else {
                    throw IOException("Virtual file is empty")
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createVirtualFileCopy(uri: Uri): InputStream? {
        return try {
            val tempFile = File.createTempFile("virtual_", ".tmp", context.cacheDir)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            FileInputStream(tempFile)
        } catch (e: Exception) {
            null
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
                fileName = file.name,
                bytesSize = size.toInt(),
                uri = fileUri,
                isLocal = true,
            )
        }

    }
}
