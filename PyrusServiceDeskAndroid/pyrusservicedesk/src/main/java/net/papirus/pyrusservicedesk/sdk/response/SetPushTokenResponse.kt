package net.papirus.pyrusservicedesk.sdk.response

internal class SetPushTokenResponse(error: ResponseError? = null)
    : ResponseBase<Unit>(error, Unit)
