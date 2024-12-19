package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.file_preview

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.graphics.PorterDuff
import android.graphics.drawable.RotateDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View
import android.view.View.*
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.webkit.*
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdActivityFilePreviewBinding
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk._ref.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Activity for previewing files
 */
internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(
    FilePreviewViewModel::class.java) {

    override val layoutResId: Int = R.layout.psd_activity_file_preview
    override val toolbarViewId: Int = R.id.file_preview_toolbar
    override val refresherViewId: Int = NO_ID
    override val progressBarViewId: Int = R.id.progress_bar

    private var pageFinishedSuccessfully = false

    private lateinit var binding: PsdActivityFilePreviewBinding

    private fun dispatch(event: FilePreviewView.Event) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // if you don't set empty text, Android will set the app name
        supportActionBar?.title = ""

        binding = PsdActivityFilePreviewBinding.bind(findViewById<View>(android.R.id.content).rootView)
        applyStyle()
        initListeners()

//        binding.toolbarTitle.text = viewModel.getFileName()
//        binding.fileExtension.text = viewModel.getExtension()

        binding.webView.apply{
            settings.apply {
                builtInZoomControls = true
                useWideViewPort = true
                setSupportZoom(true)
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptEnabled = true
            }
            webViewClient = object: WebViewClient(){
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    dispatch(FilePreviewView.Event.OnWebViewError(error))
                    // TODO
//                    viewModel.onErrorReceived()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageFinishedSuccessfully = true
                }
            }
            webChromeClient = object: WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    dispatch(FilePreviewView.Event.OnLoadProgressChanged(newProgress))
                    // TODO
//                    viewModel.onProgressChanged(newProgress)
                }
            }
        }

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
            pageFinishedSuccessfully = savedInstanceState.getBoolean(STATE_FINISHED_SUCCESSFULLY)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
        outState.putBoolean(STATE_FINISHED_SUCCESSFULLY, pageFinishedSuccessfully)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false

        MenuInflater(this).inflate(R.menu.psd_file_preview_menu, menu)
        menu.findItem(R.id.download).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.share).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.loading)?.let {
            it.setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            (it.icon as? RotateDrawable)?.animateInfinite(LOADING_ICON_ANIMATION_DURATION_MS, LinearInterpolator())
        }
        val iconColor = ConfigUtils.getToolbarButtonColor(this)
        menu.findItem(R.id.download).icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
        menu.findItem(R.id.share).icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
        menu.findItem(R.id.loading)?.icon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
        return true
    }

    override fun startObserveData() {
        super.startObserveData()
//        viewModel.getFileLiveData().observe(this) {
//            it?.let { model ->
//                when {
//                    model.isPreviewable -> applyPreviewableViewModel(model)
//                    else -> applyNonPreviewableViewModel(model)
//                }
//            }
//        }

    }

    override fun isValidPermissionRequestCode(requestCode: Int)
            = requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE

    override fun onPermissionsGranted(permissions: Array<String>) {
        super.onPermissionsGranted(permissions)

        if (permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            startDownloadFile()
    }

    private fun applyNonPreviewableViewModel(model: FileViewModel) {
        binding.webView.visibility = GONE
        binding.progressBar.visibility = GONE
        binding.noPreview.visibility = VISIBLE
        setActionBarItemVisibility(R.id.loading, model.isDownloading)
        setActionBarItemVisibility(R.id.download, !model.hasError && !model.isLocal && !model.isDownloading)
        setActionBarItemVisibility(R.id.share, model.isLocal)

        binding.downloadButton.setOnClickListener{
            when {
                model.isLocal -> dispatchLocalFileAction(model.fileUri, ACTION_VIEW)
                else -> startDownloadFile()
            }
        }

        when {
            model.isDownloading -> {
                binding.downloadButton.text = resources.getString(R.string.psd_downloading)
                binding.downloadButton.isEnabled = false
                binding.downloadButton.visibility = VISIBLE
                binding.noPreviewText.visibility = GONE
            }
            model.isLocal -> {
                if (canBePreviewedInOtherApp(model.fileUri)) {
                    binding.downloadButton.visibility = VISIBLE
                    binding.noPreviewText.visibility = GONE
                    binding.downloadButton.isEnabled = true
                    binding.downloadButton.text = resources.getString(R.string.psd_open)
                }
                else{
                    binding.downloadButton.visibility = GONE
                    binding.noPreviewText.visibility = VISIBLE
                }
            }
            else -> {
                binding.downloadButton.visibility = VISIBLE
                binding.downloadButton.text = resources.getString(R.string.psd_download)
                binding.downloadButton.isEnabled = true
                binding.noPreviewText.visibility = GONE
            }
        }

    }

    private fun applyPreviewableViewModel(model: FileViewModel) {
        binding.progressBar.visibility = VISIBLE
        binding.noPreview.visibility = GONE
        setActionBarItemVisibility(R.id.loading, model.isDownloading)
        setActionBarItemVisibility(R.id.download,!model.hasError && !model.isLocal && !model.isDownloading)
        setActionBarItemVisibility(R.id.share, model.isLocal)

        when {
            model.hasError -> {
                binding.noConnection.root.visibility = VISIBLE
                binding.webView.visibility = GONE
            }
            else -> {
                binding.webView.visibility = VISIBLE
                binding.noConnection.root.visibility = GONE
                if (!pageFinishedSuccessfully) {
                    binding.webView.loadDataWithBaseURL(
                        null,
                        """
                          <!DOCTYPE html>
                          <html>
                              <head></head>
                              <body>
                                <table style="width:100%; height:100%;">
                                  <tr>
                                    <td style="vertical-align:middle;">
                                      <img src="${model.fileUri}">
                                    </td>
                                  </tr>
                                </table>
                              </body>
                            </html>
                        """.trimIndent(),
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            }
        }
    }

    private fun setActionBarItemVisibility(itemId: Int, isVisible: Boolean) {
        // TODO check this shit
        launch {
            while (binding.filePreviewToolbar.menu.findItem(itemId) == null)
                delay(CHECK_MENU_INFLATED_DELAY_MS)
            binding.filePreviewToolbar.menu.findItem(itemId)?.isVisible = isVisible
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
        item ?: return false

        when (item.itemId) {
            R.id.download -> {
                dispatch(FilePreviewView.Event.OnDownloadClick)
                // TODO
                startDownloadFile()
            }
            R.id.share -> {
                dispatch(FilePreviewView.Event.OnShareClick)
                // TODO
//                viewModel.getFileLiveData().value?.let {
//                    dispatchLocalFileAction(it.fileUri, ACTION_SEND)
//                }
            }
        }

        return true
    }

    private fun startDownloadFile() {
//        viewModel.onDownloadFileClicked()
    }

    private fun initListeners() {
        binding.filePreviewToolbar.setNavigationOnClickListener { finish() }
        binding.filePreviewToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
    }

    private fun applyStyle() {
        binding.toolbarTitle.setTextColor(getTextColorOnBackground(this, ConfigUtils.getHeaderBackgroundColor(this)))
        binding.filePreviewToolbar.setNavigationIcon(R.drawable.psd_arrow_back)

        binding.filePreviewToolbar.navigationIcon?.setColorFilter(
            ConfigUtils.getToolbarButtonColor(this),
            PorterDuff.Mode.SRC_ATOP
        )

        ConfigUtils.getMainFontTypeface()?.let {
            binding.fileExtension.typeface = it
            binding.downloadButton.typeface = it
            binding.noPreviewText.typeface = it
        }
        ConfigUtils.getMainBoldFontTypeface()?.let {
            binding.toolbarTitle.typeface = it
        }
        val secondaryColor = getSecondaryColorOnBackground(ConfigUtils.getNoPreviewBackgroundColor(this))
        binding.fileExtension.setTextColor(getColorOnBackground(ConfigUtils.getNoPreviewBackgroundColor(this), 40))
        binding.noPreviewText.setTextColor(secondaryColor)

        binding.downloadButton.setTextColor(ConfigUtils.getAccentColor(this))

        binding.filePreviewToolbar.setBackgroundColor(ConfigUtils.getHeaderBackgroundColor(this))

        binding.noConnection.noConnectionImageView.setColorFilter(secondaryColor)
        binding.noConnection.noConnectionTextView.setTextColor(secondaryColor)
        binding.noConnection.reconnectButton.setTextColor(ConfigUtils.getAccentColor(this))
        binding.noConnection.root.setBackgroundColor(ConfigUtils.getNoConnectionBackgroundColor(this))

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ConfigUtils.getStatusBarColor(this)?: window.statusBarColor

        binding.noPreview.setBackgroundColor(ConfigUtils.getNoPreviewBackgroundColor(this))
    }

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
                PyrusServiceDesk.get().application,
                FilePreviewActivity::class.java
            ).putExtra(KEY_FILE_DATA, fileData)
        }
    }
}