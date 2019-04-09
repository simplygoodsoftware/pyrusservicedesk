package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View.*
import android.webkit.*
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_file_preview.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileData
import net.papirus.pyrusservicedesk.utils.hasPermission


/**
 * Activity for previewing files
 */
internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(FilePreviewViewModel::class.java) {

    companion object {

        internal const val KEY_FILE_DATA = "KEY_FILE_DATA"
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_file_preview_menu, menu)
            it.findItem(R.id.download).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            true
        } ?: false
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getFileLiveData().observe(
            this,
            Observer {
                it?.let { model ->
                    applyViewModel(model)
                }
            }
        )
    }

    private fun applyViewModel(model: FileViewModel) {
        when {
            model.canBePreviewed -> {
                progress_bar.visibility = VISIBLE
                no_preview.visibility = GONE
                when {
                    model.hasError -> {
                        no_connection.visibility = VISIBLE
                        web_view.visibility = GONE
                        file_preview_toolbar.menu.findItem(R.id.download)?.isVisible = false
                    }
                    else -> {
                        web_view.visibility = VISIBLE
                        no_connection.visibility = GONE
                        web_view.loadUrl(model.fileUri.toString())
                        file_preview_toolbar.menu.findItem(R.id.download)?.isVisible = true
                    }
                }
            }
            else -> {
                web_view.visibility = GONE
                progress_bar.visibility = GONE
                no_preview.visibility = VISIBLE
                file_preview_toolbar.menu.findItem(R.id.download)?.isVisible = !model.hasError && !model.isLocal
                download_button.text = if (model.isLocal) "open" else "download"
                download_button.setOnClickListener{
                    when {
                        model.isLocal -> dispatchOpenFile(model.fileUri)
                        else -> startDownloadFile()
                    }
                }
            }
        }
        invalidateOptionsMenu()
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

    private fun dispatchOpenFile(fileUri: Uri) {
        val intent = Intent(ACTION_VIEW)
        intent.setDataAndType(fileUri, contentResolver.getType(fileUri))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun onMenuItemClicked(item: MenuItem?): Boolean {
        if (item == null)
            return false
        when (item.itemId) {
            R.id.download -> startDownloadFile()
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