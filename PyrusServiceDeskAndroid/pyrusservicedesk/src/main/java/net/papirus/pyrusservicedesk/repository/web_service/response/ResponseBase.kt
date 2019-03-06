package net.papirus.pyrusservicedesk.repository.web_service.response

import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.RequestBase

internal open class ResponseBase<Request: RequestBase, ResponseData> (
        val status: Status = Status.Ok,
        val request: Request,
        val responseData: ResponseData? = null)