package net.papirus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName

internal class SetPushTokenBody(appId: String,
                                userId: String,
                                @SerializedName("device_id")
                                private val token: String)
    : RequestBodyBase(appId, userId){

    @SerializedName("type")
    private val type = "android"
}
