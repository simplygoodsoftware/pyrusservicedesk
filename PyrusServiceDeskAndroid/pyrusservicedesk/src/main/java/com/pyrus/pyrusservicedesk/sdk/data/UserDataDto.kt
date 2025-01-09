package com.pyrus.pyrusservicedesk.sdk.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Information about additional users for whom tickets need to be received.
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 * @param lastNoteId the biggest id of all comments in [userId] scope
 */
@Keep
class UserDataDto(
    @SerializedName("app_id") val appId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("security_key") val securityKey: String?,
    @SerializedName("last_note_id") val lastNoteId: Long?,
)