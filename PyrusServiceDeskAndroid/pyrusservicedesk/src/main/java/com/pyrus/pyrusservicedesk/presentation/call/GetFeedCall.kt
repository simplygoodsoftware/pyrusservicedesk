package com.pyrus.pyrusservicedesk.presentation.call

import kotlinx.coroutines.CoroutineScope
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError

/**
 * Adapter for obtaining ticket feed.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 */
internal class GetFeedCall(
        scope: CoroutineScope,
        private val requests: RequestFactory)
    : BaseCall<List<Comment>>(scope){


    override suspend fun run(): CallResult<List<Comment>> {
        var result: List<Comment>? = null
        var error: ResponseError? = null
        requests.getFeedRequest().execute(
            object : ResponseCallback<List<Comment>> {
                override fun onSuccess(data: List<Comment>) {
                    result = data
                }

                override fun onFailure(responseError: ResponseError) {
                    error = responseError
                }
            }
        )

        return CallResult(result, error)
    }
}