package net.papirus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal data class Author(
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar_id")
    val avatarId: Int = 0,
    @SerializedName("avatar_color")
    val avatarColorString: String = "#fffffff")