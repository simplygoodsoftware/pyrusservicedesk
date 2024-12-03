package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Response on [SetPushTokenRequest]
 */
internal class SetPushTokenResponse(error: ResponseError? = null) : ResponseImpl<Unit>(error, Unit)
