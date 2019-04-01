package net.papirus.pyrusservicedesk.presentation.call_adapter

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Adapter for adding comments to a ticket with the given id.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param ticketId id of ticker to add new comment to.
 * @param comment comment to be added.
 * @param uploadFileHooks file hooks that are used for managing uploading of the file in comment.
 */
internal class AddCommentCallAdapter(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        private val ticketId: Int,
        private val comment: Comment,
        private val uploadFileHooks: UploadFileHooks? = null)
    : CallAdapterBase<Int>(scope) {

    override suspend fun run(): CallResult<Int> {
        var result: Int? = null
        var error: ResponseError? = null
        requests.getAddCommentRequest(ticketId, comment, uploadFileHooks).execute(
            object: ResponseCallback<Int>{
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