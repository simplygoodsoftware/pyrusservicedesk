package net.papirus.pyrusservicedesk.sdk.response

import net.papirus.pyrusservicedesk.sdk.ResponseStatus

internal abstract class ResponseBase1<Data>(val status: ResponseStatus, val result: Data?)
