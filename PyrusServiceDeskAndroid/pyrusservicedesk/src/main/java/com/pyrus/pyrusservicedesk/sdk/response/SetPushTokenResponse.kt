package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Response on [SetPushTokenRequest]
 */
internal class SetPushTokenResponse(error: ResponseError? = null)
    : ResponseBase<Unit>(error, Unit)
