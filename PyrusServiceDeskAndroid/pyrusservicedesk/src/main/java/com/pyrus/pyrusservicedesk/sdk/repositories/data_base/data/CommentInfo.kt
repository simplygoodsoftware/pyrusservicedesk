package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Embedded

internal data class CommentInfo(
    @ColumnInfo(name = "comment_id") val commentId: Long,
    @ColumnInfo(name = "created_at") val creationDate: Long,
    @Embedded("author_") val author: AuthorEntity?,
    @ColumnInfo(name = "body") val body: String?,
    @ColumnInfo(name = "last_attachment_name") val lastAttachmentName: String?,
)