package net.papirus.pyrusservicedesk.presentation.call_adapter

import kotlinx.coroutines.CoroutineScope
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks


/**
 * Adapter for adding comments to a comment feed.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param comment comment to be added.
 * @param uploadFileHooks file hooks that are used for managing uploading of the file in comment.
 */
internal class AddFeedCommentCallAdapter(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        private val comment: Comment,
        private val uploadFileHooks: UploadFileHooks? = null)
    : CallAdapterBase<Int>(scope) {

    override suspend fun run(): CallResult<Int> {
        var result: Int? = null
        var error: ResponseError? = null
        requests.getAddFeedCommentRequest(comment, uploadFileHooks).execute(
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