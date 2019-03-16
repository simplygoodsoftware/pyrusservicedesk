package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CreateTicketRequest1(
        val userName: String,
        val description: TicketDescription,
        val uploadFileHooks: UploadFileHooks
)
