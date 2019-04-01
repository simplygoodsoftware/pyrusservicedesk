package net.papirus.pyrusservicedesk.sdk.response

/**
 * Base class that represents response of on a [RequestBase].
 * Can contain either data or an error
 */
internal abstract class ResponseBase<Data>(val error: ResponseError?, val result: Data?)
