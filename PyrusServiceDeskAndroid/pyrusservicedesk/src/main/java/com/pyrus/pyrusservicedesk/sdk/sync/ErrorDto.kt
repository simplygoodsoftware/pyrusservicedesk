package com.pyrus.pyrusservicedesk.sdk.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ErrorDto(
    @Json(name = "text") val text: String?,
    @Json(name = "code") val code: Int?,
)