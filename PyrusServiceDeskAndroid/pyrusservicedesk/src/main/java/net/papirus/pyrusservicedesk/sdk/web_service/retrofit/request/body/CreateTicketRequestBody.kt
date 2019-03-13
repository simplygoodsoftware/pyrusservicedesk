package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription

internal class CreateTicketRequestBody(
        appId: String,
        userId: String,
        @SerializedName("user_name")
        val userName: String,
        @SerializedName("ticket")
        val ticket: TicketDescription)
    : RequestBodyBase(appId, userId)
