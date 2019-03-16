package net.papirus.pyrusservicedesk.presentation.usecases.file_preview

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_file_preview.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.data.FILE_ID_EMPTY
import net.papirus.pyrusservicedesk.sdk.updates.UpdateBase
import net.papirus.pyrusservicedesk.sdk.updates.UpdateType
import net.papirus.pyrusservicedesk.sdk.web.getFileUrl
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
import net.papirus.pyrusservicedesk.presentation.navigation.UiNavigator
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase

internal class FilePreviewActivity: ConnectionActivityBase<FilePreviewViewModel>(FilePreviewViewModel::class.java) {

    companion object {
        private const val KEY_FILE_ID = "KEY_FILE_ID"

        fun getLaunchIntent(fileId: Int): Intent {
            return Intent(
                PyrusServiceDesk.getInstance().application,
                FilePreviewActivity::class.java).putExtra(KEY_FILE_ID, fileId)
        }

        private fun Intent.getFileId() = getIntExtra(KEY_FILE_ID, FILE_ID_EMPTY)
    }

    override val layoutResId: Int = R.layout.psd_activity_file_preview
    override val toolbarViewId: Int = R.id.file_preview_toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = getString(R.string.psd_organization_support, viewModel.organizationName) }
        file_preview_toolbar.setNavigationIcon(R.drawable.psd_menu)
        file_preview_toolbar.setNavigationOnClickListener { UiNavigator.toTickets(this@FilePreviewActivity) }
        file_preview_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }

        web_view.apply{
            settings.apply {
                builtInZoomControls = true
                setSupportZoom(true)
                loadWithOverviewMode = true
                useWideViewPort = true
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            menu.findItem(R.id.psd_main_menu_close).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            true
        } ?: false
    }

    override fun updateProgress(newProgress: Int) {
        super.updateProgress(newProgress)
        if (newProgress == resources.getInteger(R.integer.psd_progress_max_value))
            while (web_view.zoomOut()){}
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_main_menu_close -> sharedViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }
}

internal class FilePreviewViewModel(pyrusServiceDesk: PyrusServiceDesk): ConnectionViewModelBase(pyrusServiceDesk){
    override fun <T : UpdateBase> onUpdateReceived(update: T) {}

    override fun getUpdateTypes(): Set<UpdateType> = emptySet()

    override fun loadData() {}

    fun getWebChromeClient() =  object: WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            publishProgress(newProgress)
        }
    }
}