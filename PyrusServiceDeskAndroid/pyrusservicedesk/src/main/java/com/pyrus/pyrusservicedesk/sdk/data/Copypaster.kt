package com.pyrus.pyrusservicedesk.sdk.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import java.io.File
import java.io.FileOutputStream
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class Copypaster(
    private val context: Context,
    private val fileResolver: FileResolver
) {

    fun copyFile(uri: Uri): Uri? {

        val tempFile = createTempFile(getFileName(uri) ?: return null)

        val ous = fileResolver.getInputStream(uri) ?: return null
        ous.use { inputStream ->
            FileOutputStream(tempFile).use { fileOutputStream ->
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) {
                    fileOutputStream.write(buf, 0, len)
                }
                fileOutputStream.flush()
            }
        }

        return Uri.fromFile(tempFile)
    }

    private fun initFilesDir() {
        val fileDirs = File(getTempFilesDirPath())
        fileDirs.mkdirs()
    }

    private fun getTempFilesDirPath(): String {
        val rootDir = context.filesDir
        return rootDir.path + "/temp_files/"
    }

    private fun createTempFile(fileName: String): File {
        initFilesDir()

        val tempPath = getTempFilesDirPath() + System.currentTimeMillis() + fileName
        val tempFile = File(tempPath)

        val dir = File(tempFile.parent!!)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return tempFile
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor.use { c ->
                if (c != null && c.moveToFirst()) {
                    result = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path?: return null
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result
    }



}