package net.papirus.pyrusservicedesk.repository.web_service.response

import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.AddCommentRequest

internal class AddCommentResponse(
        status: Status = Status.Ok,
        request: AddCommentRequest,
        commentId: Int? = null)
    : ResponseBase<AddCommentRequest, Any>(status, request, commentId)
