package com.pyrus.pyrusservicedesk.sdk.data

import com.google.gson.annotations.SerializedName

/**
 * Represents an command of the comment.
 */
internal data class Command(
    @SerializedName("command_Id")
    val commandId: String,
    @SerializedName("type")
    val type: TicketCommandType,
    @SerializedName("app_id")
    val appId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("params")
    val params: Any, //use the CreateComment or MarkTicketAsRead
)