package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Command
 * @param userId user id.
 * @param appId extension id.
 * @param ticketId ticket id.
 */

@JsonClass(generateAdapter = true)
internal data class MarkTicketAsReadDto(
    @Json(name = "ticket_id") val ticketId: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "app_id") val appId: String,
)
