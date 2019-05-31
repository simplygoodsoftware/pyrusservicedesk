package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Entry for rendering comment of the comment feed.
 */
internal class CommentEntry(
        // comment to be rendered in ui
        val comment: Comment,
        // used for rendering uploading progress as well as for cancelling an upload.
        val uploadFileHooks: UploadFileHooks? = null,
        // responseError that is used for rendering errors
        var error: ResponseError? = null)
    : TicketEntry() {

    override val type: Type = Type.Comment

    /**
     * @return true if current entry contains an responseError
     */
    fun hasError() = error != null
}