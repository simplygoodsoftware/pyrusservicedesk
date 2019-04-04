package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.data.Comment

/**
 * Response on [GetFeedRequest]
 */
internal class GetFeedResponse(
    error: ResponseError? = null,
    comments: List<Comment>? = null)
    : ResponseBase<List<Comment>>(error, comments)
