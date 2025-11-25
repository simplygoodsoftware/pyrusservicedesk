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
internal data class RatingSettingsDto(
    @Json(name = "size") val size: Int?,
    @Json(name = "type") val type: Int?,
    @Json(name = "rating_text_values") val ratingTextValues: List<RatingTextValuesDto>?,
)