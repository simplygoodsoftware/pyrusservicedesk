package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus
import net.papirus.pyrusservicedesk.sdk.data.Comment

internal class GetConversationResponse1(
    status: ResponseStatus,
    comments: List<Comment>? = null)
    : ResponseBase1<List<Comment>>(status, comments)
