package net.papirus.pyrusservicedesk.repository.web_service.retrofit.request

import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.RequestBodyBase

internal abstract class RequestBase{
    abstract fun makeRequestBody(appId: String, userId: String): RequestBodyBase
}