package net.papirus.pyrusservicedesk.sdk.data

import android.net.Uri
import com.google.gson.annotations.SerializedName

internal const val FILE_ID_EMPTY = 0

internal data class Attachment(
        @SerializedName("id")
        val id:Int = FILE_ID_EMPTY,
        @SerializedName("guid")
        val guid: String = "",
        @SerializedName("type")
        val type: Int = 0,
        @SerializedName("name")
        val name: String = "",
        @SerializedName("size")
        val bytesSize: Int = 0,
        @SerializedName("is_text")
        val isText: Boolean = false,
        @SerializedName("is_video")
        val isVideo: Boolean = false,
        @Transient
        val uri: Uri? = null)