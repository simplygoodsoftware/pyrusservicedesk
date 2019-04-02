package net.papirus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName

/**
 * Request body for setting push token for the enclosing app.
 * @param token toked that should be registered.
 */
internal class SetPushTokenBody(appId: String,
                                userId: String,
                                @SerializedName("token")
                                private val token: String)
    : RequestBodyBase(appId, userId){

    @SerializedName("type")
    private val type = "android"
}
