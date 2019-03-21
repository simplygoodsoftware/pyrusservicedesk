package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CommentEntry(
        val comment: Comment,
        val uploadFileHooks: UploadFileHooks? = null,
        val onClickedCallback: OnClickedCallback<CommentEntry>,
        var error: ResponseError? = null)
    : TicketEntry() {

    override val type: Type = Type.Comment

    fun hasError() = error != null
}