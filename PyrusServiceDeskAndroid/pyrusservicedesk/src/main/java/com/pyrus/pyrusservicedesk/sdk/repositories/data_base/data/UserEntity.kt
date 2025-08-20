package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.USERS_TABLE


@Entity(
    tableName = USERS_TABLE,
    primaryKeys = ["user_id"],
    indices = [Index(value = ["app_id"], unique = false)],
    foreignKeys = [ForeignKey(
        entity = ApplicationEntity::class,
        parentColumns = ["app_id"],
        childColumns = ["app_id"],
        onDelete = ForeignKey.CASCADE
    )],
)
internal data class UserEntity(
    @ColumnInfo("user_id") val userId: String,
    @ColumnInfo("app_id") val appId: String,
    @ColumnInfo("user_name") val userName: String,
)