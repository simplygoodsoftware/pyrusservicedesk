package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Base error that can happen during request processing.
 * @param message user-friendly message
 */
internal sealed class ResponseError(message: String): Exception(message)

/**
 * Error for rendering pending offline data.
 */
internal class PendingDataError(): ResponseError("")

/**
 * Error that is happened on a server side.
 */
internal class ApiCallError(message: String): ResponseError(message)

/**
 * Error that is happened due to weak internet connection.
 */
internal class NoInternetConnection(message: String): ResponseError(message)

/**
 * Error that is happened when response contains no data. Normally can't be happened. For requests that return any data.
 */
internal class EmptyDataError : ResponseError("Data is empty (null was received)")

/**
 * Error that happens when authorization request has failed. Can happen if userId or securityKey are not valid.
 */
internal class AuthorizationError(message: String): ResponseError(message)