package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.Comment

/**
 * Response on [GetFeedRequest]
 */
internal class GetFeedResponse(
    error: ResponseError? = null,
    comments: List<Comment>? = null)
    : ResponseBase<List<Comment>>(error, comments)
