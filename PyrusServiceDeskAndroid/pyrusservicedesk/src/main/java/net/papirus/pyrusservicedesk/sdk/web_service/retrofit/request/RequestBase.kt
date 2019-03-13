package net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request

import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.body.RequestBodyBase

internal open class RequestBase{
    open fun makeRequestBody(appId: String, userId: String): RequestBodyBase {
        return RequestBodyBase(appId, userId)
    }
}