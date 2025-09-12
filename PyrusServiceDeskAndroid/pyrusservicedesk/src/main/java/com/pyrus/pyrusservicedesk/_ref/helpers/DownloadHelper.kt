package com.pyrus.pyrusservicedesk._ref.helpers;

import android.content.Context
import android.os.Environment
import com.pyrus.pyrusservicedesk._ref.utils.Try
import kotlinx.coroutines.job
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Helper that handles HTTP downloads. Thread-safe.
 */
internal class DownloadHelper (
    private val context: Context,
) {

    private val client = OkHttpClient()

    private fun getDownloadedFiles(fileDir: File): MutableList<File> {
        val savedAudioFiles = mutableListOf<File>()
        fileDir.listFiles()?.let { files ->
            savedAudioFiles.addAll(files.sortedBy { it.lastModified() })
        }
        return savedAudioFiles
    }


    /**
     * Return base folder in app private directory for the attachments files.
     */
    fun userAttachmentsDir(): File {
        return File(
            "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}"
                    + File.separator + "Attachments"
        )
    }

    private fun deleteOldestFileIfNeed(fileDir: File) {
        val savedAudioFiles = getDownloadedFiles(fileDir)
        while (savedAudioFiles.size >= MAX_FILES_COUNT) {
            val oldestFile = savedAudioFiles.firstOrNull()
            oldestFile?.delete()
            savedAudioFiles.remove(oldestFile)
        }
    }


    fun downloadFile(
        file: File, url: String,
        coroutineContext: CoroutineContext
    ): Try<Unit> {
        try {
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to download: ${response.code()}")
                }

                val body = response.body() ?: throw IOException("Empty response body")
//                val contentLength = body.contentLength()

                file.parentFile?.mkdirs()

                body.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
//                        var totalBytesRead = 0L

                        while (inputStream.read(buffer)
                                .also { bytesRead = it } != -1 && coroutineContext.job.isActive
                        ) {
                            outputStream.write(buffer, 0, bytesRead)
//                            totalBytesRead += bytesRead
//
//                            // Calculate progress
//                            val progress =
//                                if (contentLength > 0) (totalBytesRead * 100 / contentLength).toInt()
//                                else -1 // Unknown size
//
//                            onProgress(progress, totalBytesRead, contentLength)
                        }
                        if (!coroutineContext.job.isActive) {
                            throw CancellationException("Download cancelled")
                        }
                    }
                }
                file.parentFile?.let { deleteOldestFileIfNeed(it) }
                return Try.Success(Unit)
            }
        }
        catch (e: CancellationException) {
            file.delete()
            return Try.Failure(e)
        }
        catch (e: Exception) {
            return Try.Failure(e)
        }
    }

    fun deleteAllDownloadedFiles() {
        val dir = userAttachmentsDir()
        if (dir.exists()) {
            deleteFile(dir)
        }
    }

    private fun deleteFile(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteFile(child)
            }
        }

        if (!file.delete()) {
            throw IOException("Failed to delete ${file.absolutePath}")
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
        const val MAX_FILES_COUNT = 50
    }
}
