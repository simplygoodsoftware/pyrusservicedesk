package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AttachmentEntity(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "guid") val guid: String?,
    @Json(name = "bytes_size") val bytesSize: Int,
    @Json(name = "uri") val uri: Uri,

    @Transient val progress: Int? = null,
    @Transient val status: Int? = null,
)