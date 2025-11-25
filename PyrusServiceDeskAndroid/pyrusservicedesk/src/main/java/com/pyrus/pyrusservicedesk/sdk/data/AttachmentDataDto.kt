package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


/**
 * @param guid attachment quid.
 * @param type attachment type.
 * @param name attachment name.
 */

@JsonClass(generateAdapter = true)
internal data class AttachmentDataDto(
    @Json(name = "guid") val guid: String,
    @Json(name = "type") val type: Int,
    @Json(name = "name") val name: String,
)