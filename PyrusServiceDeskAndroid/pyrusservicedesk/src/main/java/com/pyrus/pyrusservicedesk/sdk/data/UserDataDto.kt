package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Information about additional users for whom tickets need to be received.
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 * @param lastNoteId the biggest id of all comments in [userId] scope
 */

@JsonClass(generateAdapter = true)
class UserDataDto(
    @Json(name = "app_id") val appId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "security_key") val securityKey: String?,
    @Json(name = "last_note_id") val lastNoteId: Long?,
)