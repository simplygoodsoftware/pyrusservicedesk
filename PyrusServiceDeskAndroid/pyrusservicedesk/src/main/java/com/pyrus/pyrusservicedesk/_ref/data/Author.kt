package com.pyrus.pyrusservicedesk._ref.data

import com.google.gson.annotations.SerializedName

internal data class Author(
    @SerializedName("name") val name: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("avatar_color") val avatarColor: String?,
)