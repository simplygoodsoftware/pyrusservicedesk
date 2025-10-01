package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ExtraUsers(
    @Json(name = "user_id") val userId: String?,
    @Json(name = "title") val title: String?,
)