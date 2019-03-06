package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.TicketDescription

internal class CreateTicketRequestBody(
        appId: String,
        userId: String,
        @SerializedName("user_name")
        val userName: String,
        @SerializedName("ticket")
        val ticket: TicketDescription)
    : RequestBodyBase(appId, userId)
