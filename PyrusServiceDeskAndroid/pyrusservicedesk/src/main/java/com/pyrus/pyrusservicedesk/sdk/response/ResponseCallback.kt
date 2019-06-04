package com.pyrus.pyrusservicedesk.sdk.response

/**
 * Callback that is passed to [RequestBase.execute] method.
 */
internal interface ResponseCallback<Data> {
    /**
     * Invoked when request was successfully executed.
     */
    fun onSuccess(data: Data)

    /**
     * Invoked when request was executed with an error.
     */
    fun onFailure(responseError: ResponseError)
}
