package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName

/**
 * Base request body for sending to the server. Contains fields that are required for almost every request.
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 */
internal open class RequestBodyBase(
        @SerializedName("app_id")
        val appId: String,
        @SerializedName("user_id")
        val userId: String,
        @SerializedName("security_key")
        val securityKey: String?,
        @SerializedName("instance_id")
        val instanceId: String?,
        @SerializedName("version")
        val version: Int
)