package net.papirus.pyrusservicedesk.presentation.usecase

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CreateTicketUseCase(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        private val comment: Comment,
        private val uploadFileHooks: UploadFileHooks? = null)
    : UseCaseBase<Int>(scope) {

    override suspend fun run(): UseCaseResult<Int> {
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
        return UseCaseResult(result, error)
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
