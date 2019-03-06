package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName

internal data class Attachment(
        @SerializedName("id")
        val id:Int = 0,
        @SerializedName("guid")
        val guid: String = "",
        @SerializedName("type")
        val type: Int = 0,
        @SerializedName("name")
        val name: String = "",
        @SerializedName("size")
        val size: Int = 0,
        @SerializedName("is_text")
        val isText: Boolean = false,
        @SerializedName("is_video")
        val isVideo: Boolean = false)