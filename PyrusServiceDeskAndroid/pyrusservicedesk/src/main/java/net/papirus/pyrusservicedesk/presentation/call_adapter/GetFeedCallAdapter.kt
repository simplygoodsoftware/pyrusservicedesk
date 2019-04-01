package net.papirus.pyrusservicedesk.presentation.call_adapter

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

/**
 * Adapter for obtaining ticket feed.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 */
internal class GetFeedCallAdapter(
        scope: CoroutineScope,
        private val requests: RequestFactory)
    : CallAdapterBase<List<Comment>>(scope){


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