package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.pyrus.pyrusservicedesk.sdk.data.UserDataDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Base request body for sending to the server. Contains fields that are required for almost every request.
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 * @param needFullInfo a flag indicating whether additional information about all tickets should be returned.
 * @param additionalUsers list of users
 * @param lastNoteId maximum comment id that is stored on the device for the current userId.
 * @param commands an array of commands about changing the state of tickets.
 * @param authorId author phone number hash.
 * @param authorName author name.
 * @param instanceId installation id on the device.
 * @param version version of service desk 1,2,or 3.
 * @param apiSign api flag.
 */

@JsonClass(generateAdapter = true)
internal open class RequestBodyBase(
    @Json(name = "need_full_info") val needFullInfo: Boolean,
    @Json(name = "additional_users") val additionalUsers: List<UserDataDto>?,
    @Json(name = "last_note_id") val lastNoteId: Long?,
    @Json(name = "commands") val commands: List<TicketCommandDto>?,
    @Json(name = "author_id") val authorId: String?,
    @Json(name = "author_name") val authorName: String?,
    @Json(name = "app_id") val appId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "security_key") val securityKey: String?,
    @Json(name = "instance_id") val instanceId: String?,
    @Json(name = "version") val version: Int,
    @Json(name = "api_sign") val apiSign: String?,
)