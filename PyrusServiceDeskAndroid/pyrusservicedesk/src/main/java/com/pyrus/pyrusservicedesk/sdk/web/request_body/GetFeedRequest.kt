package com.pyrus.pyrusservicedesk.sdk.web.request_body

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 * @param instanceId UID of application.
 * @param version API version.
 * @param keepUnread If true, comments remain unread. False - otherwise.
 */
@Keep
internal open class GetFeedBody(
        appId: String,
        userId: String,
        securityKey: String?,
        instanceId: String?,
        version: Int,
        @SerializedName("keep_unread")
        val keepUnread: Boolean,
        apiSign: String?,
): RequestBodyBase(false, null, null, null, null, appId, userId, securityKey, instanceId, version, apiSign) //TODO check