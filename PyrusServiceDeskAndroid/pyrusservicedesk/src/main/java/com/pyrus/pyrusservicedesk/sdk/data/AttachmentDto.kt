package com.pyrus.pyrusservicedesk.sdk.data

import android.net.Uri
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.gson.Local
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val FILE_ID_EMPTY = 0L

/**
 * Represents an attachment of the comment.
 * @param id attachment id.
 * @param guid attachment quid.
 * @param type attachment type.
 * @param name attachment name.
 * @param bytesSize attachment size in bytes.
 * @param isText flag indicating whether attachment is text.
 * @param isVideo flag indicating whether attachment is is video.
 * @param localUri transient field that is used only for local comments.
 */

@JsonClass(generateAdapter = true)
internal data class AttachmentDto(
    @Json(name = "id") val id: Long = FILE_ID_EMPTY,
    @Json(name = "guid") val guid: String = "",
    @Json(name = "type") val type: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "size") val bytesSize: Int = 0,
    @Json(name = "is_text") val isText: Boolean = false,
    @Json(name = "is_video") val isVideo: Boolean = false,
    @Local @Json(name = "local_uri") val localUri: Uri? = null,
    @Transient val status: Status = Status.Completed,
) {

    fun isLocal() = id == FILE_ID_EMPTY

}