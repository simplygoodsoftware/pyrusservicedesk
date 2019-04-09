package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.app.Application
import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewActivity.Companion.KEY_FILE_DATA
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileData
import net.papirus.pyrusservicedesk.utils.canBePreviewed
import net.papirus.pyrusservicedesk.utils.getExtension

/**
 * ViewModel for the file previews.
 */
internal class FilePreviewViewModel(pyrusServiceDesk: PyrusServiceDesk,
                                    private val intent: Intent) : ConnectionViewModelBase(pyrusServiceDesk){

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
            isLocalFile(),
            fileCanBePreviewed(),
            isNetworkConnected.value == false)
    }

    fun getFileLiveData(): LiveData<FileViewModel> = fileLiveData

    /**
     * Callback to be called when progress of the file downloading for preview is changed.
     *
     * @progress current progress of the file downloading
     */
    fun onProgressChanged(progress: Int) {
        if (fileLiveData.value?.hasError != true)
            publishProgress(progress)
    }

    /**
     * Callback to be called when user received an error while being downloaded the preview
     * of the attachment.
     */
    fun onErrorReceived() {
        fileLiveData.value = with(fileLiveData.value!!) {
            FileViewModel(fileUri, isLocal, canBePreviewed, true)
        }
    }

    fun onDownloadFileClicked() {
        val fileData = intent.getFileData()
        val request = DownloadManager.Request(fileData.uri)
        request.setDescription(fileData.fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileData.fileName)
        downloadRequestId = downloadManager.enqueue(request)
        observeProgress()
    }

    private fun observeProgress() {
        launch {
            var isDownloaded = false
            while(!isDownloaded) {
                delay(300)
                val c = downloadManager.query(DownloadManager.Query().setFilterById(downloadRequestId))
                if (c != null
                    && c.moveToFirst()
                    && c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {

                    fileLiveData.postValue(FileViewModel(
                        downloadManager.getUriForDownloadedFile(downloadRequestId),
                        true,
                        fileCanBePreviewed()))
                    isDownloaded = true
                }
            }
        }
    }

    fun getExtension(): String = intent.getFileData().fileName.getExtension()
    fun getFileName(): CharSequence = intent.getFileData().fileName

    private fun isLocalFile(): Boolean = intent.getFileData().isLocal
    private fun fileCanBePreviewed(): Boolean = intent.getFileData().fileName.canBePreviewed()
}

internal fun Intent.getFileData() = getParcelableExtra<FileData>(KEY_FILE_DATA)
