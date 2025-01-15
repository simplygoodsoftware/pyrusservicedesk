package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 * @param instanceId UID of application.
 * @param version API version.
 * @param keepUnread If true, comments remain unread. False - otherwise.
 */

@JsonClass(generateAdapter = true)
internal open class GetFeedBody(
    appId: String,
    userId: String,
    securityKey: String?,
    instanceId: String?,
    version: Int,
    @Json(name = "keep_unread") val keepUnread: Boolean,
    apiSign: String?,
) : RequestBodyBase(false, null, null, null, null, null, appId, userId, securityKey, instanceId, version, null)