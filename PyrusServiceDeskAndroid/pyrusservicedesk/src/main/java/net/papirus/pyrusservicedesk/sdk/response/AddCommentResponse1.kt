package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus

internal class AddCommentResponse1(
    status: ResponseStatus,
    commentId: Int? = null)
    : ResponseBase1<Int>(status, commentId)
