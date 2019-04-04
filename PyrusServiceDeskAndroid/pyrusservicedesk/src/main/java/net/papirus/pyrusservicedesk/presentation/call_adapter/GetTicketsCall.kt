package net.papirus.pyrusservicedesk.presentation.call_adapter

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

/**
 * Adapter for obtaining list of available tickets.
 * @param scope coroutine scope for executing request.
 */
internal class GetTicketsCall(
        scope: CoroutineScope,
        private val requests: RequestFactory)
    : BaseCall<List<TicketShortDescription>>(scope) {

    override suspend fun run(): CallResult<List<TicketShortDescription>> {
        var tickets: List<TicketShortDescription>? = null
        var error: ResponseError? = null
        requests.getTicketsRequest().execute(
            object: ResponseCallback<List<TicketShortDescription>> {
                override fun onSuccess(data: List<TicketShortDescription>) {
                    tickets = data
                }

                override fun onFailure(responseError: ResponseError) {
                    error = responseError
                }

            }
        )
        return CallResult(tickets, error)
    }
}