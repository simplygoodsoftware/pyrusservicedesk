package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request

import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body.AddCommentRequestBody

internal class AddCommentRequest(
        val ticketId: Int,
        val comment: String? = null,
        val attachments: List<Attachment>? = null,
        val userName: String)
    : RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): AddCommentRequestBody {
        return AddCommentRequestBody(appId, userId, comment, attachments, userName)
    }
}
