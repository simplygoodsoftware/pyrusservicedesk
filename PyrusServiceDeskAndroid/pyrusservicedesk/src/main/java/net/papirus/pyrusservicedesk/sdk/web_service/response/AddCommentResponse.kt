package net.papirus.pyrusservicedesk.sdk.web_service.response

import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.AddCommentRequest

internal class AddCommentResponse(
        status: Status = Status.Ok,
        request: AddCommentRequest,
        commentId: Int? = null)
    : ResponseBase<AddCommentRequest, Int>(status, request, commentId)
