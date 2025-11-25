package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.ATTACHMENTS_TABLE

@Entity(
    tableName = ATTACHMENTS_TABLE,
    primaryKeys = ["id"],
    indices = [
        Index(value = ["comment_id"], unique = false),
    ],
    foreignKeys = [ForeignKey(
        entity = CommentEntity::class,
        parentColumns = ["comment_id"],
        childColumns = ["comment_id"],
        onDelete = ForeignKey.CASCADE
    )],
)
internal data class AttachmentEntity(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "comment_id") val commentId: Long,
    @ColumnInfo(name = "guid") val guid: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "size") val bytesSize: Int,
    @ColumnInfo(name = "is_text") val isText: Boolean,
    @ColumnInfo(name = "is_video") val isVideo: Boolean,
    @ColumnInfo(name = "local_uri") val localUri: String?,
)