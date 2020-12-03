package com.pyrus.pyrusservicedesk.sdk.web.request_body

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Request body for setting push token for the enclosing app.
 * @param token that should be registered. If null push notifications stop.
 */
@Keep
internal class SetPushTokenBody(appId: String,
                                userId: String,
                                securityKey: String?,
                                instanceId: String?,
                                version: Int,
                                @SerializedName("token")
                                private val token: String?)
    : RequestBodyBase(appId, userId, securityKey, instanceId, version){

    @SerializedName("type")
    private val type = "android"
}
