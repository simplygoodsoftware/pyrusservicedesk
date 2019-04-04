package net.papirus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Represents author of the comment.
 * @param avatarId should be used for get avatar request see [RequestUtils.getAvatarUrl]
 */
internal data class Author(
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar_id")
    val avatarId: Int = 0,
    @SerializedName("avatar_color")
    val avatarColorString: String = "#fffffff")