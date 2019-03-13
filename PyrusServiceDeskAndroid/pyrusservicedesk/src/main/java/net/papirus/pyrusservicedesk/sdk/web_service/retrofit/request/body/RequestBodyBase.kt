package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body

import com.google.gson.annotations.SerializedName

internal open class RequestBodyBase(
        @SerializedName("app_id")
        val appId: String,
        @SerializedName("user_id")
        val userId: String)