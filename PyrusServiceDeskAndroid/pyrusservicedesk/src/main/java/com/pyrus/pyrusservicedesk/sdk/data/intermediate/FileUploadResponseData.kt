package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Intermediate data that is used for parsing response of the upload file request.
 */

@JsonClass(generateAdapter = true)
internal class FileUploadResponseData(
    @Json(name = "guid") val guid: String,
    @Json(name = "md5_hash") val hash: String?,
)