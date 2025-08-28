package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents ticket object.
 * @param appId extension id.
 * @param orgName extension name.
 * @param orgLogoUrl extension logo url.
 * @param extraUsers extension extra users.
 */
@JsonClass(generateAdapter = true)
internal data class RatingTextValuesDto(
    @Json(name = "rating") val rating: Int?,
    @Json(name = "text") val text: String?,
)