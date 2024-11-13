package com.pyrus.pyrusservicedesk.presentation.call

import kotlinx.coroutines.CoroutineScope
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError

/**
 * Adapter for obtaining ticket wit the specified [ticketId].
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param ticketId id of the desired ticket.
 */
internal class GetTicketCall(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        val ticketId: Int
) : BaseCall<Comments>(scope){


    override suspend fun run(): CallResult<Comments> {
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

        return CallResult(Comments(ticket?.comments ?: emptyList()), error)
    }
}