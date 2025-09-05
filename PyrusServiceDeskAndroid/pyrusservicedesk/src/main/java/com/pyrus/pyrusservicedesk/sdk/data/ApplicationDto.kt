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
internal data class ApplicationDto(
    @Json(name = "app_id") val appId: String?,
    @Json(name = "org_name") val orgName: String?,
    @Json(name = "org_logo_url") val orgLogoUrl: String?,
    @Json(name = "extra_users") val extraUsers: List<ExtraUsers>?,
    @Json(name = "org_description") val orgDescription: String?,
    @Json(name = "author_info") val authorsInfo: Map<String, List<AuthorInfoDto>>?,
    @Json(name = "rating_settings") val ratingSettings: RatingSettingsDto? = null,
    @Json(name = "welcome_message") val welcomeMessage: String?,
)