package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName

internal data class Author(
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar_id")
    val avatarId: Int,
    @SerializedName("avatar_color")
    val avatarColorString: String){

    fun getInitials(): String {
        return with(StringBuilder()){
            for (part in name.split(" "))
                append(part[0]);
            toString()
        }
    }
}