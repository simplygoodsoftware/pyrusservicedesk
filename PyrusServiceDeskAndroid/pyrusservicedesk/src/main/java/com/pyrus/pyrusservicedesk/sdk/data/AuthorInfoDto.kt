package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AuthorInfoDto(
    @Json(name = "author_id") val authorId: String,
    @Json(name = "name") val name: String?,
    @Json(name = "has_access") val hasAccess: Boolean?,
    @Json(name = "phone") val phone: String?,
)
