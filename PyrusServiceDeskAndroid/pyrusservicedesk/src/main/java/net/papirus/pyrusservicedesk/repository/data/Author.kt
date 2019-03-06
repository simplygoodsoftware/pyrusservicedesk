package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName

internal data class Author(
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar_id")
    val avatarId: Int,
    @SerializedName("avatar_color")
    val avatarColorString: String){


    companion object {
        fun local(userName: String) = Author(userName, 0, "")
    }
}