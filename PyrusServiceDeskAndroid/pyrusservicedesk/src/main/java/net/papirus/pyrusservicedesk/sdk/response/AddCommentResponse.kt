package net.papirus.pyrusservicedesk.sdk.response

internal class AddCommentResponse(
    error: ResponseError? = null,
    commentId: Int? = null)
    : ResponseBase<Int>(error, commentId)
