package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName

internal data class Attachment(
        @SerializedName("Guid")
        val guid: String,
        @SerializedName("Type")
        val type: Int = 0)