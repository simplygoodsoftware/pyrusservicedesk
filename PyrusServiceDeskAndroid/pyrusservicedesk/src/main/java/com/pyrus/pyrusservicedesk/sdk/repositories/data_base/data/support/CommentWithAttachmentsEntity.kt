package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import androidx.room.Embedded
import androidx.room.Relation
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentEntity

internal data class CommentWithAttachmentsEntity(
    @Embedded
    val comment: CommentEntity,
    @Relation(
        parentColumn = "comment_id",
        entityColumn = "comment_id",
    )
    val attachments: List<AttachmentEntity>
) {

    override fun toString(): String {
        return comment.body.toString()
    }
}