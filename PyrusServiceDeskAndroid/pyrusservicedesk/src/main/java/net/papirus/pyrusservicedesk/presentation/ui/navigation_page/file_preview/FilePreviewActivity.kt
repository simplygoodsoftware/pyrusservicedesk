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
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileData

private const val KEY_FILE_DATA = "KEY_FILE_DATA"


internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(FilePreviewViewModel::class.java) {

    companion object {

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

    var wasError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = intent.getFileData().fileName }
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
                    no_connection.visibility = VISIBLE
                    wasError = true
                }
            }
            webChromeClient = object: WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (!wasError) {
                        viewModel.onProgressChanged(newProgress)
                    }
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
        urlViewModel.value = intent.getFileData().uri.toString()
        urlViewModel.postValue(null)
    }

    fun getUrlViewModel(): LiveData<String> = urlViewModel

    fun onProgressChanged(progress: Int) {
        publishProgress(progress)
    }
}

internal fun Intent.getFileData() = getParcelableExtra<FileData>(KEY_FILE_DATA)
