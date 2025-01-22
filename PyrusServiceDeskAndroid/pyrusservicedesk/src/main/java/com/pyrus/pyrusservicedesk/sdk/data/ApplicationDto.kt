package com.pyrus.pyrusservicedesk.sdk.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents ticket object.
 * @param appId extension id.
 * @param orgName extension name.
 * @param orgLogoUrl extension logo url.
 */
@JsonClass(generateAdapter = true)
internal data class ApplicationDto(
    @Json(name = "app_id") val appId: String?,
    @Json(name = "org_name") val orgName: String?,
    @Json(name = "org_logo_url") val orgLogoUrl: String?,
)