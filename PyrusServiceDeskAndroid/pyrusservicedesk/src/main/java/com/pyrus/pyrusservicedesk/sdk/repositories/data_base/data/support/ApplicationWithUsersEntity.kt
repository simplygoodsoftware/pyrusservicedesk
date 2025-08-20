package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import androidx.room.Embedded
import androidx.room.Relation
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.UserEntity

internal data class ApplicationWithUsersEntity(
    @Embedded
    val application: ApplicationEntity,
    @Relation(
        entity = UserEntity::class,
        parentColumn = "app_id",
        entityColumn = "app_id",
    )
    val users: List<UserWithTicketsEntity>
)