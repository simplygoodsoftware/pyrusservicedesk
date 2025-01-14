package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.google.gson.annotations.SerializedName

internal data class AttachmentEntity(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("guid") val guid: String?,
    @SerializedName("bytes_size") val bytesSize: Int,
    @SerializedName("guid") val uri: Uri,

    // TODO ignore this fields
    @SerializedName("progress") val progress: Int?,
    @SerializedName("status") val status: Int?,
)