package com.pyrus.pyrusservicedesk._ref.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents ticket object.
 * @param name author name.
 * @param authorId author id (phone number hash).
 * @param avatarUrl author avatar url.
 * @param avatarColor author avatar color.
 */

@JsonClass(generateAdapter = true)
internal data class Author(
    @Json(name = "name") val name: String?,
    @Json(name = "author_id") val authorId: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "avatar_color") val avatarColor: String?,
)