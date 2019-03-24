package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.View.*
import android.webkit.*
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_file_preview.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewActivity.Companion.getFileId
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.sdk.data.FILE_ID_EMPTY
import net.papirus.pyrusservicedesk.sdk.getFileUrl

internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(FilePreviewViewModel::class.java) {

    companion object {
        private const val KEY_FILE_ID = "KEY_FILE_ID"
        private const val KEY_FILE_NAME = "KEY_FILE_NAME"

        fun getLaunchIntent(fileId: Int, fileName: String): Intent {
            return Intent(
                    PyrusServiceDesk.getInstance().application,
                    FilePreviewActivity::class.java)
                .putExtra(KEY_FILE_ID, fileId)
                .putExtra(KEY_FILE_NAME, fileName)
        }

        internal fun Intent.getFileId() = getIntExtra(KEY_FILE_ID, FILE_ID_EMPTY)
        private fun Intent.getFileName() = getStringExtra(KEY_FILE_NAME)
    }

    override val layoutResId: Int = R.layout.psd_activity_file_preview
    override val toolbarViewId: Int = R.id.file_preview_toolbar
    override val refresherViewId: Int = NO_ID

    var wasError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = intent.getFileName() }
        file_preview_toolbar.setNavigationIcon(R.drawable.psd_arrow_back)
        file_preview_toolbar.setNavigationOnClickListener { finish() }

        web_view.apply{
            settings.apply {
                builtInZoomControls = true
                setSupportZoom(true)
                loadWithOverviewMode = true
                useWideViewPort = true
                domStorageEnabled = true
            }
            webViewClient = object: WebViewClient(){
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    no_connection.visibility = VISIBLE
                    wasError = true
                }
            }
            webChromeClient = object: WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (!wasError)
                        viewModel.onProgressChanged(newProgress)
                }
            }
        }

        if (savedInstanceState != null)
            web_view.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        web_view.saveState(outState)
    }

    override fun observeData() {
        super.observeData()
        viewModel.getUrlViewModel().observe(
            this,
            Observer {
                it?.let { url ->
                    web_view.loadUrl(url)
                }
            }
        )
    }

    override fun reconnect() {
        super.reconnect()
        no_connection.visibility = GONE
        wasError = false
    }

    override fun updateProgress(newProgress: Int) {
        super.updateProgress(newProgress)
        if (newProgress == resources.getInteger(R.integer.psd_progress_max_value))
            while (web_view.zoomOut()){}
    }
}

internal class FilePreviewViewModel(pyrusServiceDesk: PyrusServiceDesk,
                                    private val intent: Intent)
    : ConnectionViewModelBase(pyrusServiceDesk){

    private val urlViewModel = MutableLiveData<String>()

    init {
        loadData()
    }

    override fun onLoadData() {
        urlViewModel.value = getFileUrl(intent.getFileId())
        urlViewModel.postValue(null)
    }

    fun getUrlViewModel(): LiveData<String> = urlViewModel

    fun onProgressChanged(progress: Int) {
        publishProgress(progress)
    }
}