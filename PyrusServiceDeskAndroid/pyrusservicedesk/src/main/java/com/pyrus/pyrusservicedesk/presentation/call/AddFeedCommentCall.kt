package com.pyrus.pyrusservicedesk.presentation.call

import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import kotlinx.coroutines.CoroutineScope


/**
 * Adapter for adding comments to a comment feed.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param comment comment to be added.
 * @param uploadFileHooks file hooks that are used for managing uploading of the file in comment.
 */
internal class AddFeedCommentCall(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        private val ticketId: Int,
        private val comment: Comment,
        private val uploadFileHooks: UploadFileHooks? = null)
    : BaseCall<AddCommentResponseData>(scope) {

    override suspend fun run(): CallResult<AddCommentResponseData> {
        var result: AddCommentResponseData? = null
        var error: ResponseError? = null
        requests.getAddFeedCommentRequest(ticketId, comment, uploadFileHooks).execute(
            object: ResponseCallback<AddCommentResponseData> {
                override fun onSuccess(data: AddCommentResponseData) {
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