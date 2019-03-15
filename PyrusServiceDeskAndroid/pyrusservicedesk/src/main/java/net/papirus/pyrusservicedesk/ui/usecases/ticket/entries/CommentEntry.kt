package net.papirus.pyrusservicedesk.ui.usecases.ticket.entries

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.updates.UpdateError
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.FileUploadCallbacks

internal class CommentEntry(
    val comment: Comment,
    val uploadCallbacks: FileUploadCallbacks? = null,
    val onClickedCallback: OnClickedCallback<CommentEntry>,
    val updateError: UpdateError? = null)
    : TicketEntry() {

    override val type: Type = Type.Comment

    fun hasError() = updateError != null
}