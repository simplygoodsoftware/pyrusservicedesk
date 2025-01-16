package com.pyrus.pyrusservicedesk.sdk.sync

import com.google.gson.annotations.SerializedName

internal data class ErrorDto(
    @SerializedName("text") val text: String?,
    @SerializedName("code") val code: Int?,
)