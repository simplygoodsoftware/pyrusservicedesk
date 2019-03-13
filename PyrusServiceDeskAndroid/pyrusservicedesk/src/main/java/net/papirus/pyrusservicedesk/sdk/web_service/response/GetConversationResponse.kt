package net.papirus.pyrusservicedesk.sdk.web_service.response

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.RequestBase

internal class GetConversationResponse(
        status: Status = Status.Ok,
        request: RequestBase,
        comments: List<Comment>? = null)
    : ResponseBase<RequestBase, List<Comment>>(status, request) {

}
