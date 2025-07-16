package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import com.google.gson.annotations.SerializedName

data class RatingTextValues (
    @SerializedName("rating")
    val rating: Int?,

    @SerializedName("text")
    val text: String?,
)