package net.papirus.pyrusservicedesk.presentation.usecase

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

internal class GetTicketsUseCase(
        scope: CoroutineScope,
        private val requests: RequestFactory)
    : UseCaseBase<List<TicketShortDescription>>(scope) {

    override suspend fun run(): UseCaseResult<List<TicketShortDescription>> {
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
        return UseCaseResult(tickets, error)
    }
}