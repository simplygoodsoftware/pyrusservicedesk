package com.pyrus.pyrusservicedesk.presentation.call

import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Command
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import kotlinx.coroutines.CoroutineScope

/**
 * Adapter for obtaining list of available tickets.
 * @param scope coroutine scope for executing request.
 */
internal class GetTicketsCall(
    scope: CoroutineScope,
    private val requests: RequestFactory,
    private val commands: List<Command> = emptyList()
) : BaseCall<Tickets>(scope) {

    override suspend fun run(): CallResult<Tickets> {
        var getTicketsResult: Tickets? = null
        var error: ResponseError? = null
        requests.getTicketsRequest(commands).execute(
            object: ResponseCallback<Tickets> {
                override fun onSuccess(data: Tickets) {
                    getTicketsResult = data
                }

                override fun onFailure(responseError: ResponseError) {
                    error = responseError
                }

            }
        )
        return CallResult(getTicketsResult, error)
    }
}