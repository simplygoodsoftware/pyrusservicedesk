package net.papirus.pyrusservicedesk.sdk.data.intermediate

import android.net.Uri

internal data class FileData(val fileName: String, val bytesSize: Int, val uri: Uri)