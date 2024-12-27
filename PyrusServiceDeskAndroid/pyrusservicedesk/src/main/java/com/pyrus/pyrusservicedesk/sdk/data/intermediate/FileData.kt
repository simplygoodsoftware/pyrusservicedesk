package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * Intermediate data for transfer attachment data between UI elements.
 * @param uri can contain either url of the server attachment or uri of the local file.
 */
@Keep
@Parcelize
internal data class FileData(
    val fileName: String,
    val bytesSize: Int,
    val uri: Uri,
    val isLocal: Boolean,
) : Parcelable