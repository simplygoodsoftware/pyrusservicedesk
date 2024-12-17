package com.pyrus.pyrusservicedesk._ref.data

import android.net.Uri
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

internal data class Attachment(
    val id: Int,
    val name: String,
    val isImage: Boolean,
    val isText: Boolean,
    val bytesSize: Int,
    val isVideo: Boolean,
    val uri: Uri,
    val status: Status,
)