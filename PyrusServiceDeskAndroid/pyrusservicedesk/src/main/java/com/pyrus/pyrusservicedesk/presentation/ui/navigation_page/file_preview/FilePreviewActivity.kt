package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.View.*
import android.webkit.*
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import kotlinx.android.synthetic.main.psd_activity_file_preview.*

private const val KEY_FILE_DATA = "KEY_FILE_DATA"

/**
 * Activity for previewing files
 */
internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(FilePreviewViewModel::class.java) {

    companion object {

        /**
         * Provides intent for launching the activity.
         *
         * @param fileData data of the attachment to be reviewed.
         * @return intent to be used for launching the preview.
         */
        fun getLaunchIntent(fileData: FileData): Intent {
            return Intent(
                    PyrusServiceDesk.getInstance().application,
                    FilePreviewActivity::class.java)
                .putExtra(KEY_FILE_DATA, fileData)
        }
    }

    override val layoutResId: Int = R.layout.psd_activity_file_preview
    override val toolbarViewId: Int = R.id.file_preview_toolbar
    override val refresherViewId: Int = NO_ID
    override val progressBarViewId: Int = R.id.progress_bar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            title = intent.getFileData().fileName
        }
        file_preview_toolbar.setNavigationIcon(R.drawable.psd_arrow_back)
        file_preview_toolbar.setNavigationOnClickListener { finish() }

        web_view.apply{
            settings.apply {
                builtInZoomControls = true
                setSupportZoom(true)
                useWideViewPort = true
                javaScriptEnabled = true
            }
            webViewClient = object: WebViewClient(){
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    viewModel.onErrorReceived()
                }
            }
            webChromeClient = object: WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    viewModel.onProgressChanged(newProgress)
                }
            }
        }

        if (savedInstanceState != null) {
            web_view.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        web_view.saveState(outState)
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getUrlLiveData().observe(
            this,
            Observer {
                it?.let { url ->
                    web_view.loadUrl(url)
                }
            }
        )

        viewModel.getHasErrorLiveData().observe(
            this,
            Observer {
                it?.let { hasError ->
                    no_connection.visibility = if (hasError) VISIBLE else GONE
                }
            }
        )
    }

    override fun updateProgress(newProgress: Int) {
        super.updateProgress(newProgress)
        if (newProgress == resources.getInteger(R.integer.psd_progress_max_value))
            while (web_view.zoomOut()){}
    }
}


/**
 * ViewModel for the file previews.
 */
internal class FilePreviewViewModel(pyrusServiceDesk: PyrusServiceDesk,
                                    private val intent: Intent)
    : ConnectionViewModelBase(pyrusServiceDesk){

    private val urlViewModel = MutableLiveData<String>()

    private var hasError = MutableLiveData<Boolean>()

    init {
        loadData()
    }

    override fun onLoadData() {
        hasError.value = false
        urlViewModel.value = intent.getFileData().uri.toString()
        urlViewModel.postValue(null)
    }

    /**
     * Provides liva data with url of the file to be shown.
     *
     * @return live data with url string to be observed
     */
    fun getUrlLiveData(): LiveData<String> = urlViewModel

    /**
     * Provides live data with the error state of the file previewing.
     *
     * @return live data with the error state of the previewing. Normally this value
     * is null.
     */
    fun getHasErrorLiveData(): LiveData<Boolean> = hasError

    /**
     * Callback to be called when progress of the file downloading for preview is changed.
     *
     * @progress current progress of the file downloading
     */
    fun onProgressChanged(progress: Int) {
        if (hasError.value != true)
            publishProgress(progress)
    }

    /**
     * Callback to be called when user received an error while being downloaded the preview
     * of the attachment.
     */
    fun onErrorReceived() {
        hasError.value = true
    }
}

private fun Intent.getFileData() = getParcelableExtra<FileData>(KEY_FILE_DATA)
