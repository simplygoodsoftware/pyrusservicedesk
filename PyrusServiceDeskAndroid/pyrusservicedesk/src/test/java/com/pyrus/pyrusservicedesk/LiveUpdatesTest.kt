package com.pyrus.pyrusservicedesk

import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.updates.LastComment
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.Preferences
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import org.junit.Before
import org.junit.Test
import java.util.*

class LiveUpdatesTest {

    private lateinit var liveUpdates: LiveUpdates

    @Before
    fun initTestGround() {
        liveUpdates = LiveUpdates(
            createRequestFactoryMock(),
            createCustomPreferencesMock(),
            null
        )
    }

    @Test
    fun test() {
        LiveUpdates
    }

    private fun createRequestFactoryMock(
        getFeedResponseQueue: Queue<Response<Comments>>,
        getTicketsResponseQueue: Queue<GetTicketsResponse>
    ): RequestFactory {
        return RequestFactory(object : GeneralRepository {

            override suspend fun getFeed(): Response<Comments> {
                return if (getFeedResponseQueue.size == 1)
                    getFeedResponseQueue.element()
                else
                    getFeedResponseQueue.poll()!!
            }

            override suspend fun getTickets(): GetTicketsResponse {
                return if (getFeedResponseQueue.size == 1)
                    getTicketsResponseQueue.element()
                else
                    getTicketsResponseQueue.poll()!!
            }

            override suspend fun getTicket(ticketId: Int, isActive: Boolean?): GetTicketResponse {
                TODO("Not yet implemented")
            }

            override suspend fun addComment(
                ticketId: Int,
                comment: Comment,
                uploadFileHooks: UploadFileHooks?
            ): Response<AddCommentResponseData> {
                TODO("Not yet implemented")
            }

            override suspend fun addFeedComment(
                comment: Comment,
                uploadFileHooks: UploadFileHooks?
            ): Response<AddCommentResponseData> {
                TODO("Not yet implemented")
            }

            override suspend fun createTicket(
                description: TicketDescription,
                uploadFileHooks: UploadFileHooks?
            ): CreateTicketResponse {
                TODO("Not yet implemented")
            }

            override suspend fun setPushToken(token: String?): SetPushTokenResponse {
                TODO("Not yet implemented")
            }

            override suspend fun addPendingFeedComment(comment: Comment): Response<Boolean> {
                TODO("Not yet implemented")
            }

            override suspend fun getPendingFeedComments(): Response<Comments> {
                TODO("Not yet implemented")
            }

            override suspend fun removePendingComment(comment: Comment): Response<Boolean> {
                TODO("Not yet implemented")
            }

            override suspend fun removeAllPendingComments() {
                TODO("Not yet implemented")
            }

        })
    }

    private fun createCustomPreferencesMock(defaultComment: LastComment? = null, defaultLastActiveTime: Long = -1): Preferences {

        return object : Preferences {

            private var lastComment = defaultComment
            private var lastActiveTime = defaultLastActiveTime

            override fun saveLastComment(comment: LastComment) {
                lastComment = comment
            }

            override fun getLastComment(): LastComment? {
                return lastComment
            }

            override fun removeLastComment() {
                lastComment = null
            }

            override fun saveLastActiveTime(time: Long) {
                lastActiveTime = time
            }

            override fun getLastActiveTime(): Long {
                return lastActiveTime
            }

        }
    }
}