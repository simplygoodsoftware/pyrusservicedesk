package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Response on [AddCommentRequest]
 */
internal class AddCommentResponse(
    error: ResponseError? = null,
    commentId: Int? = null)
    : ResponseImpl<Int>(error, commentId)
