package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import androidx.room.Embedded
import androidx.room.Relation
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.UserEntity

internal data class UserWithTicketsEntity(
    @Embedded
    val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id",
    )
    val tickets: List<TicketEntity>,
)