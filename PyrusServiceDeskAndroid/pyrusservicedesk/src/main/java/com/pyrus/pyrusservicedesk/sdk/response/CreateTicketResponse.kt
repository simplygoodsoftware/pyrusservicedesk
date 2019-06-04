package com.pyrus.pyrusservicedesk.sdk.response

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CreateTicketResponseData

/**
 * Response on [CreateTicketRequest]
 */
internal class CreateTicketResponse(
    error: ResponseError? = null,
    data: CreateTicketResponseData? = null)
    : ResponseBase<CreateTicketResponseData>(error, data)
