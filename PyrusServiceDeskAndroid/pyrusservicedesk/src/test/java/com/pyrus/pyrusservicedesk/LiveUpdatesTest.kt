package com.pyrus.pyrusservicedesk

import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.pyrus.pyrusservicedesk.MockitoExtensions.kAny
import com.pyrus.pyrusservicedesk.MockitoExtensions.kEq
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.Author
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.updates.LastComment
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.Preferences
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_SECOND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.verification.VerificationMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class LiveUpdatesTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Mock
    internal lateinit var newReplySubscriber: NewReplySubscriber

    @JvmField
    @Rule
    var mockitoRule = MockitoJUnit.rule()!!

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        PyrusServiceDesk.init(getApplicationContext(), APP_ID)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun onStartHasNewCommentTest() {
        val liveUpdates = createLiveUpdatesWithTwoComments(false)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        checkNewReplySubscriber(true)
    }

    @Test
    fun onStartHasNewShowedCommentTest() {
        val liveUpdates = createLiveUpdatesWithTwoComments(false, lastShowedIsShown = true)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        checkNewReplySubscriber(true, Mockito.never())
    }

    @Test
    fun onStartHasNewCommentChatIsActiveTest() {
        val liveUpdates = createLiveUpdatesWithTwoComments(false)
        liveUpdates.increaseActiveScreenCount()
        liveUpdates.subscribeOnReply(newReplySubscriber)
        checkNewReplySubscriber(true, Mockito.never())
    }

    @Test
    fun onStartNoNewCommentsTest() {
        val liveUpdates = createLiveUpdatesWithTwoComments(true)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        checkNewReplySubscriber(false)
    }

    @Test
    fun notifyTwiceTest() {
        val liveUpdates = createLiveUpdatesWithTwoComments(false)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        checkNewReplySubscriber(true, Mockito.after(5L * MILLISECONDS_IN_SECOND).only())
    }

    @Test
    fun throttlingOneAndHalfMinutesTest() {
        val getTicketsResponse = createGetTicketsResponse(SERVER_FIRST_COMMENT_ID, false)
        val liveUpdates = createLiveUpdatesWithTwoComments(getTicketsResponse, 1L * MILLISECONDS_IN_MINUTE)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        then(getTicketsResponse).should(Mockito.timeout(5000 + THRESHOLD).times(2)).getData()
    }

    @Test
    fun throttlingFiveMinutesTest() {
        val getTicketsResponse = createGetTicketsResponse(SERVER_FIRST_COMMENT_ID, false)
        val liveUpdates = createLiveUpdatesWithTwoComments(getTicketsResponse, 3L * MILLISECONDS_IN_MINUTE)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        then(getTicketsResponse).should(Mockito.timeout(15000 + THRESHOLD).times(2)).getData()
    }

    @Test
    fun throttlingHourTest() {
        val getTicketsResponse = createGetTicketsResponse(SERVER_FIRST_COMMENT_ID, false)
        val liveUpdates = createLiveUpdatesWithTwoComments(getTicketsResponse, 30L * MILLISECONDS_IN_MINUTE)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        then(getTicketsResponse).should(Mockito.timeout(60000 + THRESHOLD).times(2)).getData()
    }

    @Test
    fun throttlingThreeDaysTest() {
        val getTicketsResponse = createGetTicketsResponse(SERVER_FIRST_COMMENT_ID, false)
        val liveUpdates = createLiveUpdatesWithTwoComments(getTicketsResponse, 2L * MILLISECONDS_IN_DAY)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        then(getTicketsResponse).should(Mockito.timeout(5L * MILLISECONDS_IN_MINUTE + THRESHOLD).times(2)).getData()
    }

    @Test
    fun throttlingMoreThreeDaysTest() {
        val getTicketsResponse = createGetTicketsResponse(SERVER_FIRST_COMMENT_ID, false)
        val liveUpdates = createLiveUpdatesWithTwoComments(getTicketsResponse, 4L * MILLISECONDS_IN_DAY)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        then(getTicketsResponse).should(Mockito.never()).getData()
    }

    @Test
    fun unsubscribeTest() {
        val liveUpdates = createLiveUpdatesWithTwoComments(false)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        liveUpdates.unsubscribeFromReplies(newReplySubscriber)
        liveUpdates.subscribeOnReply(newReplySubscriber)
        checkNewReplySubscriber(true)
    }

    private fun checkNewReplySubscriber(
        hasNewComments: Boolean? = null,
        mode: VerificationMode = Mockito.timeout(TIMEOUT).only()
    ) {
        then(newReplySubscriber).should(mode).onNewReply(
            if (hasNewComments != null) kEq(hasNewComments) else Mockito.anyBoolean(),
            kAny(),
            Mockito.anyInt(),
            kAny(),
            Mockito.anyLong()
        )
    }

    private fun createLiveUpdatesWithTwoComments(
        serverCommentIsRead: Boolean,
        lastActivityTimePassed: Long = 10L * MILLISECONDS_IN_SECOND,
        lastShowedIsShown: Boolean = false
    ): LiveUpdates {
        val currentTime = System.currentTimeMillis()
        val lastUserActiveTime = currentTime - lastActivityTimePassed

        return createLiveUpdates(
            createGetTicketsResponse(SERVER_FIRST_COMMENT_ID, serverCommentIsRead),
            lastUserActiveTime,
            listOf(
                createComment(USER_FIRST_COMMENT_ID, lastUserActiveTime, true),
                createComment(SERVER_FIRST_COMMENT_ID, currentTime - 5 * 1000, false)
            ),
            if (lastShowedIsShown)
                LastComment(
                    SERVER_SECOND_COMMENT_ID,
                    serverCommentIsRead,
                    true,
                    "",
                    null,
                    0,
                    currentTime - 5 * 1000
                )
            else
                null
        )
    }

    private fun createLiveUpdatesWithTwoComments(
        getTicketsResponse: GetTicketsResponse,
        lastActivityTimePassed: Long = 10L * MILLISECONDS_IN_SECOND,
        lastComment: LastComment? = null
    ): LiveUpdates {
        val currentTime = System.currentTimeMillis()
        val lastUserActiveTime = currentTime - lastActivityTimePassed

        return createLiveUpdates(
            getTicketsResponse,
            lastUserActiveTime,
            listOf(
                createComment(USER_FIRST_COMMENT_ID, lastUserActiveTime, true),
                createComment(SERVER_FIRST_COMMENT_ID, currentTime - 5 * 1000, false)
            ),
            lastComment
        )
    }

    private fun createGetTicketsResponse(
        lastCommentId: Int,
        isRead: Boolean,
        fromUser: Boolean = false,
        error: ResponseError? = null
    ): GetTicketsResponse {
        val mock: GetTicketsResponse = Mockito.mock(GetTicketsResponse::class.java)
        val lastComment = createComment(lastCommentId, 3L, fromUser)
        BDDMockito.`when`(mock.getData()).thenReturn(listOf(TicketShortDescription(TICKET_ID, COMMENT_BODY, isRead, lastComment)))
        BDDMockito.`when`(mock.getError()).thenReturn(error)
        return mock
    }

    private fun createLiveUpdates(
        getTicketsResponse: GetTicketsResponse,
        lastUserActiveTime: Long,
        comments: List<Comment>?,
        lastComment: LastComment?,
        error: ResponseError? = null
    ): LiveUpdates {
        return createLiveUpdates(
            RequestFactory(GeneralRepositoryMock(
                createGetFeedResponse(comments, error),
                getTicketsResponse
            )),
            lastUserActiveTime,
            lastComment
        )
    }

    private fun createLiveUpdates(
        requestFactory: RequestFactory,
        lastUserActiveTime: Long,
        lastComment: LastComment?
    ): LiveUpdates {
        return LiveUpdates(
            requestFactory,
            createCustomPreferencesMock(lastUserActiveTime, lastComment),
            null,
            mainThreadSurrogate,
            mainThreadSurrogate
        )
    }

    private fun createComment(id: Int, createTime: Long, fromUser: Boolean): Comment {
        return Comment(id, "1", fromUser, null, Date(createTime), Author(AUTHOR_NAME))
    }

    private fun createGetFeedResponse(comments: List<Comment>?, error: ResponseError? = null): Response<Comments> {
        return ResponseImpl(error, Comments(comments ?: emptyList()))
    }

    private class GeneralRepositoryMock(
        var getFeedResponse: Response<Comments>,
        var getTicketsResponse: GetTicketsResponse
    ): GeneralRepository {

        override suspend fun getFeed(keepUnread: Boolean): Response<Comments> {
            return getFeedResponse
        }

        override suspend fun getTickets(): GetTicketsResponse {
           return getTicketsResponse
        }

        override suspend fun getTicket(ticketId: Int): GetTicketResponse {
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

    }

    private fun createCustomPreferencesMock(defaultLastActiveTime: Long = -1, defaultLastComment: LastComment? = null): Preferences {

        return object : Preferences {

            private var lastActiveTime = defaultLastActiveTime
            private var lasComment: LastComment? = defaultLastComment

            override fun saveLastComment(comment: LastComment) {
                lasComment = comment
            }

            override fun getLastComment(): LastComment? {
                return lasComment
            }

            override fun removeLastComment() {
                lasComment = null
            }

            override fun saveLastActiveTime(time: Long) {
                lastActiveTime = time
            }

            override fun getLastActiveTime(): Long {
                return lastActiveTime
            }

        }
    }

    companion object {
        private const val TICKET_ID = 1
        private const val AUTHOR_NAME = "68"
        private const val APP_ID = "1"
        private const val COMMENT_BODY = "121"
        private const val TIMEOUT = 50L
        private const val THRESHOLD = 4000L
        private const val NO_COMMENT_ID = 0
        private const val USER_FIRST_COMMENT_ID = 1
        private const val SERVER_FIRST_COMMENT_ID = 2
        private const val SERVER_SECOND_COMMENT_ID = 3
    }

}