package com.pyrus.pyrusservicedesk.presentation.call

import kotlinx.coroutines.CoroutineScope
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Adapter for creating ticket.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param comment comment to be added as initial comment of the ticket.
 * @param uploadFileHooks file hooks that are used for managing uploading of the file in comment.
 */
internal class CreateTicketCall(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        private val comment: Comment,
        private val uploadFileHooks: UploadFileHooks? = null)
    : BaseCall<Int>(scope) {

    override suspend fun run(): CallResult<Int> {
        var result: Int? = null
        var error: ResponseError? = null
        requests.getCreateTicketRequest(comment.toTicketDescription(), uploadFileHooks).execute(
            object: ResponseCallback<Int> {
                override fun onSuccess(data: Int) {
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

private fun Comment.toTicketDescription(): TicketDescription {
    val body = when {
        !body.isEmpty() -> body
        !attachments.isNullOrEmpty() -> attachments[0].name
        else -> ""
    }
    return TicketDescription(body, body, attachments)
}
