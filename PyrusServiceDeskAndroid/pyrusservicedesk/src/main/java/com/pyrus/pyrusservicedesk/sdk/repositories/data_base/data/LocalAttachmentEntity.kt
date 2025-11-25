package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase

/**
 * @param id attachment id.
 * @param name attachment name.
 * @param guid  attachment quid.
 * @param bytesSize  attachment size in bytes .
 * @param uri  attachment uri.
 */

@Entity(
    SdDatabase.LOCAL_ATTACHMENTS_TABLE,
    primaryKeys = ["id"],
    indices = [Index(value = ["command_id"], unique = false)],
    foreignKeys = [ForeignKey(
        entity = CommandEntity::class,
        parentColumns = ["command_id"],
        childColumns = ["command_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class LocalAttachmentEntity @JvmOverloads constructor(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "command_id") val commandId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "guid") val guid: String?,
    @ColumnInfo(name = "bytes_size") val bytesSize: Int,
    @ColumnInfo(name = "uri") val uri: String,

    @Ignore val progress: Int? = null,
    @Ignore val status: Int? = null,
)