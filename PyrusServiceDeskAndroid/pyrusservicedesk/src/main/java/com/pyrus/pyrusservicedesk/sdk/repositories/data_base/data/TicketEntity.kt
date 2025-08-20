package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.TICKETS_TABLE

@Entity(
    tableName = TICKETS_TABLE,
    primaryKeys = ["ticket_id"],
    indices = [
        Index(value = ["ticket_id"], unique = true),
        Index(value = ["user_id"], unique = false),
    ],
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["user_id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
)
internal data class TicketEntity(
    @ColumnInfo(name = "ticket_id") val ticketId: Long,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "subject") val subject: String,
    @ColumnInfo(name = "unescaped_subject") val unescapedSubject: String,
    @ColumnInfo(name = "author") val author: String?,
    @ColumnInfo(name = "is_read") val isRead: Boolean?,
    @Embedded(prefix = "last_comment_") val lastComment: CommentInfo?,
    @ColumnInfo(name = "is_active") val isActive: Boolean?,
    @ColumnInfo(name = "created_at") val createdAt: Long?,
    @ColumnInfo(name = "show_rating") val showRating: Boolean?,
    @ColumnInfo(name = "show_rating_text") val showRatingText: String?,
) : TicketHeaderEntity