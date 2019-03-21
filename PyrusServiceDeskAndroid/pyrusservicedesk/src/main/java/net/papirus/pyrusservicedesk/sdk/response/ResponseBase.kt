package net.papirus.pyrusservicedesk.sdk.response

internal abstract class ResponseBase<Data>(val error: ResponseError?, val result: Data?)
