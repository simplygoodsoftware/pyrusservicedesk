package com.pyrus.pyrusservicedesk._ref.data

import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

internal data class Attachment(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("is_image") val isImage: Boolean,
    @SerializedName("is_text") val isText: Boolean,
    @SerializedName("bytes_size") val bytesSize: Int,
    @SerializedName("is_video") val isVideo: Boolean,
    @SerializedName("uri") val uri: Uri,
    @SerializedName("status") val status: Status,
    @SerializedName("progress") val progress: Int?,
    @SerializedName("guid") val guid: String?,
)