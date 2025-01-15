package com.pyrus.pyrusservicedesk._ref.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Author(
    @Json(name = "name") val name: String?,
    @Json(name = "author_id") val authorId: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "avatar_color") val avatarColor: String?,
)