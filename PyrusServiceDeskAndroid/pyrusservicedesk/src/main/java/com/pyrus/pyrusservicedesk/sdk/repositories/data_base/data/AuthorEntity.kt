package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo

internal data class AuthorEntity(
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "id") val id: String?,
    @ColumnInfo(name = "avatar_id") val avatarId: Int?,
    @ColumnInfo(name = "avatar_color") val avatarColorString: String?,
)