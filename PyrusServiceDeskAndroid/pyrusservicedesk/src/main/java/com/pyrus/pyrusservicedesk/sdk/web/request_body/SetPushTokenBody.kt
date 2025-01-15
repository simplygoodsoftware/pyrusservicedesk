package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for setting push token for the enclosing app.
 * @param token that should be registered. If null push notifications stop.
 * @param tokenType cloud messaging type.
 */

@JsonClass(generateAdapter = true)
internal class SetPushTokenBody(
    appId: String,
    userId: String,
    securityKey: String?,
    instanceId: String?,
    version: Int,
    @Json(name = "token") val token: String?,
    @Json(name = "type") val tokenType: String,
) : RequestBodyBase(false, null,null, null, null, null, appId, userId, securityKey, instanceId, version, null)
