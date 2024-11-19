package com.pyrus.pyrusservicedesk.sdk.data

import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.sdk.data.gson.Local
import com.pyrus.pyrusservicedesk.utils.RequestUtils

/**
 * Represents an attachment of the comment.
 * @param localUri transient field that is used only for local comments.
 */
internal data class Command(
    @SerializedName("command_Id")
    val commandId: String,
    @SerializedName("type")
    val type: TicketCommandType,
    @SerializedName("params")
    val params: Int = 0,
)