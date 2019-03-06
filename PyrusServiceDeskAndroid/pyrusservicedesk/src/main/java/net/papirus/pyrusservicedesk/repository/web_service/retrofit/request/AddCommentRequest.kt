package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request

import net.papirus.pyrusservicedesk.repository.data.Attachment
import net.papirus.pyrusservicedesk.repository.data.Comment
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.AddCommentRequestBody

internal class AddCommentRequest(
        val ticketId: Int,
        val comment: String? = null,
        val attachments: List<Attachment>? = null)
    : RequestBase() {

    override fun makeRequestBody(appId: String, userId: String): AddCommentRequestBody {
        return AddCommentRequestBody(appId, userId, comment, attachments)
    }
}
