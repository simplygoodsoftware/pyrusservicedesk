package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.app.Application
import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
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
internal class FilePreviewViewModel(pyrusServiceDesk: PyrusServiceDesk,
                                    private val intent: Intent)
    : ConnectionViewModelBase(pyrusServiceDesk) {

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
        fileLiveData.value = when {
            fileCanBePreviewed() -> PreviewableFileViewModel(
                intent.getFileData().uri,
                isNetworkConnected.value == false
            )
            else -> NonPreviewableViewModel(
                intent.getFileData().uri,
                isNetworkConnected.value == false,
                isLocal = isLocalFile()
            )
        }
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
        }
        publishProgress(progress)
    }

    /**
     * Callback to be called when user received an error while being downloaded the preview
     * of the attachment.
     */
    fun onErrorReceived() {
        fileLiveData.value = with(fileLiveData.value!!) {
            when (this) {
                is PreviewableFileViewModel -> PreviewableFileViewModel(
                    fileUri,
                    true
                )
                is NonPreviewableViewModel -> NonPreviewableViewModel(
                    fileUri,
                    true,
                    isLocal = isLocal
                )
            }
        }
    }

    /**
     * Callback to be invoked when download file ui was clicked
     */
    fun onDownloadFileClicked() {
        fileLiveData.value = with(fileLiveData.value!!) {
            when (this) {
                is PreviewableFileViewModel -> PreviewableFileViewModel(
                    fileUri,
                    hasError,
                    true
                )
                is NonPreviewableViewModel -> NonPreviewableViewModel(
                    fileUri,
                    hasError,

                    true
                )
            }
        }
        val fileData = intent.getFileData()
        val request = DownloadManager.Request(fileData.uri)
        request.setDescription(fileData.fileName)
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileData.fileName)
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
            when (fileLiveData.value!!) {
                is PreviewableFileViewModel ->
                    PreviewableFileViewModel(
                        intent.getFileData().uri,
                        false,
                        false
                    )

                is NonPreviewableViewModel ->
                    NonPreviewableViewModel(
                        intent.getFileData().uri,
                        false,
                        false
                    )
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
                        when (fileLiveData.value!!) {
                            is PreviewableFileViewModel ->
                                PreviewableFileViewModel(
                                    uri,
                                    false,
                                    false,
                                    true
                                )
                            is NonPreviewableViewModel ->
                                NonPreviewableViewModel(
                                    uri,
                                    false,
                                    false,
                                    true
                                )
                        }
                    )

                }
            else -> fileLiveData.postValue(
                when (fileLiveData.value!!) {
                    is PreviewableFileViewModel ->
                        PreviewableFileViewModel(
                            fileUri,
                            false,
                            false,
                            true
                        )
                    is NonPreviewableViewModel ->
                        NonPreviewableViewModel(
                            fileUri,
                            false,
                            false,
                            true
                        )
                }
            )
        }

    }

    private fun isLocalFile(): Boolean = intent.getFileData().isLocal
    private fun fileCanBePreviewed(): Boolean = intent.getFileData().fileName.canBePreviewed()
}

internal fun Intent.getFileData() = getParcelableExtra<FileData>(KEY_FILE_DATA)
