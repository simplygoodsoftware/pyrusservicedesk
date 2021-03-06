package com.pyrus.pyrusservicedesk.presentation.call

import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import kotlinx.coroutines.CoroutineScope

/**
 * Adapter for obtaining ticket feed.
 * @param scope coroutine scope for executing request.
 * @param requests factory to obtain request from.
 * @param keepUnread True - keep comments unread. False - otherwise.
 */
internal class GetFeedCall(
        scope: CoroutineScope,
        private val requests: RequestFactory,
        private val keepUnread: Boolean = false
)
    : BaseCall<Comments>(scope){


    override suspend fun run(): CallResult<Comments> {
        val response = requests.getFeedRequest(keepUnread).execute()
        return CallResult(response.getData(), response.getError())
    }
}