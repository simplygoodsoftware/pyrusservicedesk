package com.pyrus.pyrusservicedesk.sdk.repositories

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class DraftEntity(
    @Json(name = "ticket_id") val ticketId: Long,
    @Json(name = "text") val text: String,
)