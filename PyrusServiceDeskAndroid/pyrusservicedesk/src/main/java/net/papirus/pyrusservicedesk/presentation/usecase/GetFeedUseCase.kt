package net.papirus.pyrusservicedesk.presentation.usecase

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

internal class GetFeedUseCase(
        scope: CoroutineScope,
        private val requests: RequestFactory)
    : UseCaseBase<List<Comment>>(scope){


    override suspend fun run(): UseCaseResult<List<Comment>> {
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

        return UseCaseResult(result, error)
    }
}