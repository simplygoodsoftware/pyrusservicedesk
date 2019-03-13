package net.papirus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName

internal class FileUploadData(
        @SerializedName("guid")
        val guid: String,
        @SerializedName("md5_hash")
        val hash: String)