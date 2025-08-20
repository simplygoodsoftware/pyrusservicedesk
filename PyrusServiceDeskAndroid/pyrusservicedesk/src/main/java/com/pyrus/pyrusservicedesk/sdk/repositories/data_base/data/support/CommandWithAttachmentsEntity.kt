package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import androidx.room.Embedded
import androidx.room.Relation
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.LocalAttachmentEntity

internal data class CommandWithAttachmentsEntity(
    @Embedded
    val command: CommandEntity,
    @Relation(
        parentColumn = "command_id",
        entityColumn = "command_id",
    )
    val attachments: List<LocalAttachmentEntity>?,
) {

    override fun toString(): String {
        return command.comment.toString()
    }
}