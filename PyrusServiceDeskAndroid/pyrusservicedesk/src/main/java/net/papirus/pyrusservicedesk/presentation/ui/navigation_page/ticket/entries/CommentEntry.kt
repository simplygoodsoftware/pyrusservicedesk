package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Entry for rendering comment of the comment feed.
 */
internal class CommentEntry(
        // comment to be rendered in ui
        val comment: Comment,
        // used for rendering uploading progress as well as for cancelling an upload.
        val uploadFileHooks: UploadFileHooks? = null,
        // callback to be invoked on comment click with the given entry when UI is not
        // processed it by itself
        val onClickedCallback: OnClickedCallback<CommentEntry>,
        // error that is used for rendering errors
        var error: ResponseError? = null)
    : TicketEntry() {

    override val type: Type = Type.Comment

    /**
     * @return true if current entry contains an error
     */
    fun hasError() = error != null
}