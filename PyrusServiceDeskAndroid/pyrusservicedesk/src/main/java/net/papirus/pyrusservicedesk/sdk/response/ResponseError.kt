package net.papirus.pyrusservicedesk.sdk.response

internal sealed class ResponseError(message: String): Exception(message)
internal class ApiCallError(message: String): ResponseError(message)
internal class NoInternetConnection(message: String): ResponseError(message)
internal class EmptyDataError : ResponseError("Data is empty (null was received)")