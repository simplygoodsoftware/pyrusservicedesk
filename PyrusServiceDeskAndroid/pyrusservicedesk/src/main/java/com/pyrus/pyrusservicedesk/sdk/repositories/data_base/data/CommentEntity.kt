package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMENTS_TABLE
import com.pyrus.pyrusservicedesk.sdk.sync.SystemCommentType

@Entity(
    tableName = COMMENTS_TABLE,
    primaryKeys = ["comment_id"],
    indices = [
        Index(value = ["comment_id"], unique = true),
        Index(value = ["ticket_id"], unique = false),
    ],
)
internal data class CommentEntity(
    @ColumnInfo(name = "comment_id") val commentId: Long,
    @ColumnInfo(name = "ticket_id") val ticketId: Long,
    @ColumnInfo(name = "body") val body: String?,
    @ColumnInfo(name = "unescaped_body") val unescapedBody: String?,
    @ColumnInfo(name = "is_inbound") val isInbound: Boolean,
    @ColumnInfo(name = "created_at") val creationDate: Long,
    @ColumnInfo(name = "rating") val rating: Int?,
    @Embedded("author_") val author: AuthorEntity?,
    @ColumnInfo(name = "is_system") val isSystem: Boolean,
    @ColumnInfo(name = "system_comment_type") val systemCommentType: Int
) {
    override fun toString(): String {
        return body.toString()
    }
}