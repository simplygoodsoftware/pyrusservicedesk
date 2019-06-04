package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Base class that represents response of on a [RequestBase].
 * Can contain either data or an error
 */
internal open class ResponseImpl<Data>(val responseError: ResponseError?, val result: Data?): Response<Data> {

    companion object {
        /**
         * Success response creator.
         */
        fun <T> success(result: T) = ResponseImpl(null, result)

        /**
         * Unsuccess response creator.
         */
        fun <T> failure(error: ResponseError) = ResponseImpl<T>(error, null)
    }

    override fun getData(): Data? = result

    override fun getError(): ResponseError? = responseError
}
