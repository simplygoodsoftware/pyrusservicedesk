package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName

/**
 * Request body for setting push token for the enclosing app.
 * @param token that should be registered. If null push notifications stop.
 */
internal class SetPushTokenBody(appId: String,
                                userId: String,
                                secretKey: String?,
                                instanceId: String?,
                                version: Int,
                                @SerializedName("token")
                                private val token: String?)
    : RequestBodyBase(appId, userId, secretKey, instanceId, version){

    @SerializedName("type")
    private val type = "android"
}
