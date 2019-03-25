package net.papirus.pyrusservicedesk.sdk

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.support.v4.content.ContentResolverCompat
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileData
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadRequestData

internal class FileResolver(private val contentResolver: ContentResolver) {

    fun getUploadFileData(fileUri: Uri): FileUploadRequestData? {
        val cursor = ContentResolverCompat.query(
            contentResolver,
            fileUri,
            null,
            null,
            null,
            null,
            null)
        if (!cursor.moveToFirst())
            return null
        return FileUploadRequestData(
            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)),
            contentResolver.openInputStream(fileUri)
        )
    }

    fun getFileData(fileUri: Uri?): FileData? {
        if (fileUri == null)
            return null
        val cursor = ContentResolverCompat.query(
            contentResolver,
            fileUri,
            null,
            null,
            null,
            null,
            null)
        if (!cursor.moveToFirst())
            return null
        var size = cursor.getInt(cursor.getColumnIndex(OpenableColumns.SIZE))
        if (size == 0) {
            contentResolver.openInputStream(fileUri).use {
                it?.let { stream ->
                    size = stream.available()
                }
            }
        }
        return FileData(
            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)),
            size,
            fileUri)
    }
}
