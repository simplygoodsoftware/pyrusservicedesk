package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

internal data class AttachmentDataDto(
    @SerializedName("guid") val guid: String,
    @SerializedName("type") val type: Int,
    @SerializedName("name") val name: String,
)