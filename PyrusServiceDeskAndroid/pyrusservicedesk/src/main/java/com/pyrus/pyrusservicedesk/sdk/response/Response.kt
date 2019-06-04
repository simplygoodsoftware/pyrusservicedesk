package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Response is the result of the processing of a [Request]
 */
internal interface Response<T> {

    /**
     * Data that response contains.
     * May be null if an error is occurred
     */
    fun getData(): T?

    /**
     * Error that response contains.
     * @return NULL if the response is the result of successfully processed [Request]
     */
    fun getError(): ResponseError?

    /**
     * @return TRUE if response contains an error
     */
    fun hasError() = getError() != null
}