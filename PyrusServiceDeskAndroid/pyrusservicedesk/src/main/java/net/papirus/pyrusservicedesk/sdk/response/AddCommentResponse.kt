package net.papirus.pyrusservicedesk.sdk.response

/**
 * Response on [AddCommentRequest]
 */
internal class AddCommentResponse(
    error: ResponseError? = null,
    commentId: Int? = null)
    : ResponseBase<Int>(error, commentId)
