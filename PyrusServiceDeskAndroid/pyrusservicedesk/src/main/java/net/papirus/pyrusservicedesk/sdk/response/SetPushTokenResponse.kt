package net.papirus.pyrusservicedesk.sdk.response

/**
 * Response on [SetPushTokenRequest]
 */
internal class SetPushTokenResponse(error: ResponseError? = null)
    : ResponseBase<Unit>(error, Unit)
