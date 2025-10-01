package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import androidx.room.Embedded
import androidx.room.Relation
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity


internal data class TicketWithComments(
    @Embedded
    val ticket: TicketEntity,
    @Relation(
        entity = CommentEntity::class,
        parentColumn = "ticket_id",
        entityColumn = "ticket_id",
    )
    val comments: List<CommentWithAttachmentsEntity>,
)