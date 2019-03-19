package net.papirus.pyrusservicedesk.presentation.usecase

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

internal class GetTicketUseCase(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        val ticketId: Int)
    : UseCaseBase<List<Comment>>(scope){


    override suspend fun run(): UseCaseResult<List<Comment>> {
        var ticket: Ticket? = null
        var error: ResponseError? = null
        requests.getTicketRequest(ticketId).execute(
            object : ResponseCallback<Ticket> {
                override fun onSuccess(data: Ticket) {
                    ticket = data
                }

                override fun onFailure(responseError: ResponseError) {
                    error = responseError
                }
            }
        )

        return UseCaseResult(ticket?.comments, error)
    }
}