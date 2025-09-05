package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import com.pyrus.pyrusservicedesk._ref.data.RatingSettings
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.APPLICATIONS_TABLE


@Entity(
    tableName = APPLICATIONS_TABLE,
    primaryKeys = ["app_id"],
    indices = [Index(value = ["app_id"], unique = true)]
)
internal data class ApplicationEntity(
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "org_name") val orgName: String?,
    @ColumnInfo(name = "org_logo_url") val orgLogoUrl: String?,
    @ColumnInfo(name = "org_description") val orgDescription: String?,
    @Embedded(prefix = "rating_settings_") val ratingSettings: RatingSettings?,
    @ColumnInfo(name = "welcome_message") val welcomeMessage: String?,
)