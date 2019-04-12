package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.graphics.drawable.RotateDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View.*
import android.view.animation.LinearInterpolator
import android.webkit.*
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.utils.hasPermission
import com.pyrus.pyrusservicedesk.utils.animateInfinite
import com.pyrus.pyrusservicedesk.utils.hasPermission
import kotlinx.android.synthetic.main.psd_activity_file_preview.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Activity for previewing files
 */
internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(
    FilePreviewViewModel::class.java) {

    companion object {

        internal const val KEY_FILE_DATA = "KEY_FILE_DATA"
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1

        private const val CHECK_MENU_INFLATED_DELAY_MS = 100L
        private const val LOADING_ICON_ANIMATION_DURATION_MS = 1000L

        private const val STATE_FINISHED_SUCCESSFULLY = "STATE_FINISHED_SUCCESSFULLY"

        /**
         * Provides intent for launching the activity.
         *
         * @param fileData data of the attachment to be reviewed.
         * @return intent to be used for launching the preview.
         */
        fun getLaunchIntent(fileData: FileData): Intent {
            return Intent(
                PyrusServiceDesk.getInstance().application,
                FilePreviewActivity::class.java
            )
                .putExtra(KEY_FILE_DATA, fileData)
        }
    }

    override val layoutResId: Int = R.layout.psd_activity_file_preview
    override val toolbarViewId: Int = R.id.file_preview_toolbar
    override val refresherViewId: Int = NO_ID
    override val progressBarViewId: Int = R.id.progress_bar

    private var pageFinishedSuccessfully = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            title = viewModel.getFileName()
        }
        file_preview_toolbar.setNavigationIcon(R.drawable.psd_arrow_back)
        file_preview_toolbar.setNavigationOnClickListener { finish() }
        file_preview_toolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        file_extension.text = viewModel.getExtension()

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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageFinishedSuccessfully = true
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
            pageFinishedSuccessfully = savedInstanceState.getBoolean(STATE_FINISHED_SUCCESSFULLY)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if(outState == null)
            return
        web_view.saveState(outState)
        outState.putBoolean(STATE_FINISHED_SUCCESSFULLY, pageFinishedSuccessfully)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{ menu ->
            MenuInflater(this).inflate(R.menu.psd_file_preview_menu, menu)
            menu.findItem(R.id.download).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.share).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.loading)?.let {
                it.setShowAsAction(SHOW_AS_ACTION_ALWAYS)
                (it.icon as? RotateDrawable)?.animateInfinite(LOADING_ICON_ANIMATION_DURATION_MS, LinearInterpolator())
            }
            true
        } ?: false
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getFileLiveData().observe(
            this,
            Observer {
                it?.let { model ->
                    when (model){
                        is PreviewableFileViewModel -> applyPreviewableViewModel(model)
                        is NonPreviewableViewModel -> applyNonPreviewableViewModel(model)
                    }
                }
            }
        )
    }

    override fun updateProgress(newProgress: Int) {
        super.updateProgress(newProgress)
        if (newProgress == resources.getInteger(R.integer.psd_progress_max_value))
            while (web_view.zoomOut()){}
    }

    override fun isValidPermissionRequestCode(requestCode: Int)
            = requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE

    override fun onPermissionsGranted(permissions: Array<String>) {
        super.onPermissionsGranted(permissions)
        if (permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            startDownloadFile()
    }

    private fun applyNonPreviewableViewModel(model: NonPreviewableViewModel) {
        web_view.visibility = GONE
        progress_bar.visibility = GONE
        no_preview.visibility = VISIBLE
        setActionBarItemVisibility(R.id.loading, model.isDownloading)
        setActionBarItemVisibility(R.id.download, !model.hasError && !model.isLocal && !model.isDownloading)
        setActionBarItemVisibility(R.id.share, model.isLocal)

        download_button.setOnClickListener{
            when {
                model.isLocal -> dispatchLocalFileAction(model.fileUri, ACTION_VIEW)
                else -> startDownloadFile()
            }
        }

        when {
            model.isDownloading -> {
                download_button.text = resources.getString(R.string.psd_downloading)
                download_button.isEnabled = false
                download_button.visibility = VISIBLE
                no_preview_text.visibility = GONE
            }
            model.isLocal -> {
                if (canBePreviewedInOtherApp(model.fileUri)) {
                    download_button.visibility = VISIBLE
                    no_preview_text.visibility = GONE
                    download_button.isEnabled = true
                    download_button.text = resources.getString(R.string.psd_open)
                }
                else{
                    download_button.visibility = GONE
                    no_preview_text.visibility = VISIBLE
                }
            }
            else -> {
                download_button.visibility = VISIBLE
                download_button.text = resources.getString(R.string.psd_download)
                download_button.isEnabled = true
                no_preview_text.visibility = GONE
            }
        }

    }

    private fun applyPreviewableViewModel(model: PreviewableFileViewModel) {
        progress_bar.visibility = VISIBLE
        no_preview.visibility = GONE
        setActionBarItemVisibility(R.id.loading, model.isDownloading)
        setActionBarItemVisibility(R.id.download,!model.hasError && !model.isLocal && !model.isDownloading)
        setActionBarItemVisibility(R.id.share, model.isLocal)

        when {
            model.hasError -> {
                no_connection.visibility = VISIBLE
                web_view.visibility = GONE
            }
            else -> {
                web_view.visibility = VISIBLE
                no_connection.visibility = GONE
                if (!pageFinishedSuccessfully)
                    web_view.loadUrl(model.fileUri.toString())
            }
        }
    }

    private fun setActionBarItemVisibility(itemId: Int, isVisible: Boolean) {
        launch {
            while (file_preview_toolbar.menu.findItem(itemId) == null)
                delay(CHECK_MENU_INFLATED_DELAY_MS)
            file_preview_toolbar.menu.findItem(itemId)?.isVisible = isVisible
        }
    }

    private fun canBePreviewedInOtherApp(fileUri: Uri): Boolean {
        return Intent(ACTION_VIEW)
            .setDataAndType(fileUri, contentResolver.getType(fileUri))
            .resolveActivity(packageManager) != null
    }

    private fun dispatchLocalFileAction(fileUri: Uri, intentAction: String) {
        Intent(intentAction)
            .setDataAndType(fileUri, contentResolver.getType(fileUri))
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).also {
                if (intentAction == ACTION_SEND)
                    it.putExtra(Intent.EXTRA_STREAM, fileUri)
                if (it.resolveActivity(packageManager) != null) {
                    startActivity(it)
                }
            }
    }

    private fun onMenuItemClicked(item: MenuItem?): Boolean {
        if (item == null)
            return false
        when (item.itemId) {
            R.id.download -> startDownloadFile()
            R.id.share -> {
                viewModel.getFileLiveData().value?.let {
                    dispatchLocalFileAction(it.fileUri, ACTION_SEND)
                }
            }
        }
        return true
    }

    private fun startDownloadFile() {
        when{
            hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> viewModel.onDownloadFileClicked()
            else -> requestPermissionsCompat(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
        }
    }
}