package net.papirus.pyrusservicedesk.presentation.usecases.ticket.entries

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.updates.UpdateError
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CommentEntry(
    val comment: Comment,
    val uploadFileHooks: UploadFileHooks? = null,
    val onClickedCallback: OnClickedCallback<CommentEntry>,
    val updateError: UpdateError? = null)
    : TicketEntry() {

    override val type: Type = Type.Comment

    fun hasError() = updateError != null
}