package com.pyrus.pyrusservicedesk.sdk.web.request_body

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Request body for setting push token for the enclosing app.
 * @param token that should be registered. If null push notifications stop.
 * @param tokenType cloud messaging type.
 */
@Keep
internal class SetPushTokenBody(
    appId: String,
    userId: String,
    securityKey: String?,
    instanceId: String?,
    version: Int,
    @SerializedName("token")
    private val token: String?,
    @SerializedName("type")
    private val tokenType: String
) : RequestBodyBase(false,null, null, null, null, appId, userId, securityKey, instanceId, version, null) //TODO check
