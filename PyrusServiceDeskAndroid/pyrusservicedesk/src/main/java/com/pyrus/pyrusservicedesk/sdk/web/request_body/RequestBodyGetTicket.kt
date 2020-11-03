package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName

internal class RequestBodyGetTicket(
    appId: String,
    userId: String,
    securityKey: String?,
    instanceId: String?,
    version: Int,
    @SerializedName("is_active")
    val isActive: Boolean?
) : RequestBodyBase(appId, userId, securityKey, instanceId, version)