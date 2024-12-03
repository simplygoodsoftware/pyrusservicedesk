package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.file_preview

import android.net.Uri
import android.webkit.WebResourceError

internal interface FilePreviewView {

    data class Model(
        val fileUri: Uri,
        val isPreviewable: Boolean,
        val hasError: Boolean = false,
        val isDownloading: Boolean = false,
        val isLocal: Boolean = false,
    )

    sealed interface Event {
        data class OnLoadProgressChanged(val newProgress: Int) : Event

        data class OnWebViewError(val error: WebResourceError?) : Event

        object OnShareClick : Event

        object OnDownloadClick : Event

    }

}