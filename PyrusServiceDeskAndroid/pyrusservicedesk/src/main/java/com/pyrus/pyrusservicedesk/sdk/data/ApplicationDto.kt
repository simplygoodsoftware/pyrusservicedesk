package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Represents ticket object.
 */
internal data class ApplicationDto(
    @SerializedName("app_id") val appId: String?,
    @SerializedName("org_name") val orgName: String?,
    @SerializedName("org_logo_url") val orgLogoUrl: String?,
)