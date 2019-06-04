package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData

/**
 * Response on [AddCommentRequest]
 */
internal class AddCommentResponse(
    error: ResponseError? = null,
    commentData: AddCommentResponseData? = null)
    : ResponseBase<AddCommentResponseData>(error, commentData)
