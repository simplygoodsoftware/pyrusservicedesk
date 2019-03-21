package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.content.Intent
import android.os.Bundle
import android.view.View.NO_ID
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_file_preview.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
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

        private fun Intent.getFileId() = getIntExtra(KEY_FILE_ID, FILE_ID_EMPTY)
        private fun Intent.getFileName() = getStringExtra(KEY_FILE_NAME)
    }

    override val layoutResId: Int = R.layout.psd_activity_file_preview
    override val toolbarViewId: Int = R.id.file_preview_toolbar
    override val refresherViewId: Int = NO_ID

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
            webViewClient = WebViewClient()
            webChromeClient = viewModel.getWebChromeClient()
        }

        if (savedInstanceState == null)
            web_view.loadUrl(getFileUrl(intent.getFileId()))
        else
            web_view.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        web_view.saveState(outState)
    }


    override fun updateProgress(newProgress: Int) {
        super.updateProgress(newProgress)
        if (newProgress == resources.getInteger(R.integer.psd_progress_max_value))
            while (web_view.zoomOut()){}
    }
}

internal class FilePreviewViewModel(pyrusServiceDesk: PyrusServiceDesk): ConnectionViewModelBase(pyrusServiceDesk){
    override fun onLoadData() {}

    fun getWebChromeClient() = object: WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            publishProgress(newProgress)
        }
    }
}