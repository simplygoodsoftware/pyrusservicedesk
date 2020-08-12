package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.app.Application
import android.app.DownloadManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import com.pyrus.pyrusservicedesk.ServiceDeskProvider
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewActivity.Companion.KEY_FILE_DATA
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.utils.canBePreviewed
import com.pyrus.pyrusservicedesk.utils.getExtension
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for the file previews.
 */
internal class FilePreviewViewModel(serviceDeskProvider: ServiceDeskProvider,
                                    private val intent: Intent)
    : ConnectionViewModelBase(serviceDeskProvider) {

    private companion object {
        const val CHECK_FILE_DOWNLOADED_DELAY_MS = 300L
    }

    private val fileLiveData = MutableLiveData<FileViewModel>()

    private var downloadRequestId: Long = -1
    private val downloadManager by lazy {
        ((getApplication() as Application).getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
    }

    init {
        loadData()
    }

    override fun onLoadData() {
        fileLiveData.value = FileViewModel(
            intent.getFileData().uri,
            fileCanBePreviewed(),
            isNetworkConnected.value == false,
            isLocal = isLocalFile())
    }

    /**
     * Provides live data with file model
     */
    fun getFileLiveData(): LiveData<FileViewModel> = fileLiveData

    /**
     * Callback to be called when progress of the file downloading for preview is changed.
     *
     * @progress current progress of the file downloading
     */
    fun onProgressChanged(progress: Int) {
        if (fileLiveData.value?.hasError != true) {
            publishProgress(progress)
        }
    }

    /**
     * Callback to be called when user received an error while being downloaded the preview
     * of the attachment.
     */
    fun onErrorReceived() {
        fileLiveData.value = with(fileLiveData.value!!) {
            FileViewModel(fileUri, isPreviewable, hasError = true, isLocal = isLocal)
        }
    }

    /**
     * Callback to be invoked when download file ui was clicked
     */
    fun onDownloadFileClicked() {
        fileLiveData.value = with(fileLiveData.value!!) {
            FileViewModel(fileUri, isPreviewable, hasError, isDownloading = true)
        }
        val fileData = intent.getFileData()
        val request = DownloadManager.Request(fileData.uri).apply {
            setTitle(fileData.fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        downloadRequestId = downloadManager.enqueue(request)
        observeProgress()
    }

    /**
     * Provides file extension
     */
    fun getExtension(): String = intent.getFileData().fileName.getExtension()

    /**
     * Provides file name without path
     */
    fun getFileName(): CharSequence = intent.getFileData().fileName

    private fun observeProgress() {
        launch {
            var isCompleted = false
            while (!isCompleted) {
                delay(CHECK_FILE_DOWNLOADED_DELAY_MS)
                val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadRequestId))
                if (cursor == null || !cursor.moveToFirst()) {
                    processDownloadingFailedAsync()
                    break
                }
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        processDownloadedFileUriAsync(downloadManager.getUriForDownloadedFile(downloadRequestId))
                        isCompleted = true
                    }
                    DownloadManager.STATUS_FAILED ->{
                        processDownloadingFailedAsync()
                        isCompleted = true
                    }
                }
            }
        }
    }

    private fun processDownloadingFailedAsync(){
        fileLiveData.postValue(
            with(fileLiveData.value!!) {
                FileViewModel(fileUri, isPreviewable, hasError = false, isDownloading = false)
            }
        )
    }

    private fun processDownloadedFileUriAsync(fileUri: Uri?) {
        if (fileUri == null) {
            processDownloadingFailedAsync()
            return
        }
        when (fileUri.scheme) {
            ContentResolver.SCHEME_FILE ->
                MediaScannerConnection.scanFile(
                    getApplication(),
                    arrayOf(fileUri.path),
                    arrayOf(downloadManager.getMimeTypeForDownloadedFile(downloadRequestId))
                ) { _, uri ->

                    fileLiveData.postValue(
                        with(fileLiveData.value!!) {
                            FileViewModel(uri, isPreviewable, hasError = false, isDownloading = false, isLocal = true)
                        }
                    )
                }
            else -> fileLiveData.postValue(
                with(fileLiveData.value!!) {
                    FileViewModel(fileUri, isPreviewable, hasError = false, isDownloading = false, isLocal = true)
                }
            )
        }

    }

    private fun isLocalFile(): Boolean = intent.getFileData().isLocal
    private fun fileCanBePreviewed(): Boolean = intent.getFileData().fileName.canBePreviewed()
}

internal fun Intent.getFileData() = getParcelableExtra<FileData>(KEY_FILE_DATA)
