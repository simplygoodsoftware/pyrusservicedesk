package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class AddCommentRequest1(
    val ticketId: Int,
    val comment: Comment,
    val uploadFileHooks: UploadFileHooks
)
