package com.pyrus.pyrusservicedesk.sdk.data

import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.data.gson.Local
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

private const val FILE_ID_EMPTY = 0

/**
 * Represents an attachment of the comment.
 * @param localUri transient field that is used only for local comments.
 */
internal data class AttachmentDto(
    @SerializedName("id") val id: Int = FILE_ID_EMPTY,
    @SerializedName("guid") val guid: String = "",
    @SerializedName("type") val type: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val bytesSize: Int = 0,
    @SerializedName("is_text") val isText: Boolean = false,
    @SerializedName("is_video") val isVideo: Boolean = false,
    @Local @SerializedName("local_uri") val localUri: Uri? = null,
    @Transient val status: Status = Status.Completed,
) {

    fun isLocal() = id == FILE_ID_EMPTY

}