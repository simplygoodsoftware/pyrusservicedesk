package net.papirus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName

internal open class RequestBodyBase(
        @SerializedName("app_id")
        val appId: String,
        @SerializedName("user_id")
        val userId: String)