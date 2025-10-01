package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents author of the comment.
 * @param avatarId should be used for get avatar request see [RequestUtils.getAvatarUrl]
 */

@JsonClass(generateAdapter = true)
internal data class AuthorDto(
    @Json(name = "name") val name: String?,
    @Json(name = "author_id") val authorId: String?,
    @Json(name = "avatar_id") val avatarId: Int?,
    @Json(name = "avatar_color") val avatarColorString: String?,
)