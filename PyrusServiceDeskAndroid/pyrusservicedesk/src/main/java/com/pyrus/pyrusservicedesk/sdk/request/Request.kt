package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.response.Response

/**
 * Request that can be executed by the sdk.
 */
internal interface Request<U> {
    /**
     * Launches processing of the request that is always finishes with [Response] which
     * contains an responseError if any responseError occurred
     */
    suspend fun execute(): Response<U>
}