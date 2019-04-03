package net.papirus.pyrusservicedesk.presentation.call_adapter

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError

/**
 * Adapter for obtaining ticket wit the specified [ticketId].
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param ticketId id of the desired ticket.
 */
internal class GetTicketCall(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        val ticketId: Int)
    : BaseCall<List<Comment>>(scope){


    override suspend fun run(): CallResult<List<Comment>> {
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

        return CallResult(ticket?.comments, error)
    }
}