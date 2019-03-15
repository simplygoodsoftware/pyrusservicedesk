package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.FileUploadCallbacks

internal class CommentAddedUpdate(
        ticketId: Int = EMPTY_TICKET_ID,
        comment: Comment,
        val isNew: Boolean = false,
        val fileUploadCallbacks: FileUploadCallbacks? = null,
        error: UpdateError? = null)
    : CommentUpdateBase(ticketId, comment, error) {

    override val type = UpdateType.CommentAdded
}