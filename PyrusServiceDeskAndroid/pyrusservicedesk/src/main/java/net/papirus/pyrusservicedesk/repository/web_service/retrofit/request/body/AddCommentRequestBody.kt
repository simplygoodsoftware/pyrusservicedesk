package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body

import net.papirus.pyrusservicedesk.repository.data.Attachment
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.RequestBodyBase

internal class AddCommentRequestBody(
        appId: String,
        userId: String,
        val TicketId: Int,
        val UserName: String? = null,
        val Comment: String? = null,
        val Attachments: List<Attachment>? = null)
    : RequestBodyBase(appId, userId)
