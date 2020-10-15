package com.pyrus.pyrusservicedesk.sdk.web.request_body

import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription

/**
 * Request body for sending create ticket request to the server.
 */
internal class CreateTicketRequestBody(
        appId: String,
        userId: String,
        secretKey: String?,
        instanceId: String?,
        version: Int,
        @SerializedName("user_name")
        val userName: String,
        @SerializedName("ticket")
        val ticket: TicketDescription)
    : RequestBodyBase(appId, userId, secretKey, instanceId, version)
