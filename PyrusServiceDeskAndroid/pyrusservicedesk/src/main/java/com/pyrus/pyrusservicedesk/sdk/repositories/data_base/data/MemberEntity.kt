package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.MEMBERS_TABLE

@Entity(
    tableName = MEMBERS_TABLE,
    primaryKeys = ["id"],
    indices = [Index("user_id")]
)

internal data class MemberEntity(
    @ColumnInfo("id") val id: String,
    @ColumnInfo("user_id") val userId: String,
    @ColumnInfo("author_id") val authorId: String,
    @ColumnInfo("name") val name: String?,
    @ColumnInfo("has_access") val hasAccess: Boolean,
    @ColumnInfo("phone") val phone: String?,
)