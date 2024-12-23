package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Represents author of the comment.
 * @param avatarId should be used for get avatar request see [RequestUtils.getAvatarUrl]
 */
internal data class AuthorDto(
    @SerializedName("name") val name: String?,
    @SerializedName("author_id") val authorId: String?,
    @SerializedName("avatar_id") val avatarId: Int?,
    @SerializedName("avatar_color") val avatarColorString: String?,
)