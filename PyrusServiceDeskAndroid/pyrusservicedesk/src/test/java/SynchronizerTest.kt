import InitData.TEST_APP_ID
import InitData.TEST_TICKET_ID
import InitData.calcOperatorTimeRequest
import InitData.createCommentRequest
import InitData.markTicketIsReadRequest
import InitData.setPushTokenRequest
import Responses.calcOperatorTime
import Responses.createComment
import Responses.createCommentError
import Responses.emptyTickets
import Responses.markTicketIsRead
import Responses.setPushTokenResponse
import android.content.Context
import android.content.SharedPreferences
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.HttpException
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk.core.ResourceContextWrapper
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer.Companion.FAILED_AUTHORIZATION_ERROR_CODE
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer.Companion.FAILED_AUTHORIZATION_ERROR_CODE_FORBIDDEN
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer.Companion.FAILED_SYNC_ERROR_CODE
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer.Companion.NO_UPDATES
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SynchronizerTest {
    private lateinit var synchronizer: Synchronizer
    private lateinit var localTicketsStore: LocalTicketsStore
    private lateinit var accountStore: AccountStore
    private lateinit var resourceManager: AppResourceManager
    private lateinit var idStore: IdStore
    private lateinit var localCommandsStore: LocalCommandsStore
    private lateinit var accessDeniedEventBus: AccessDeniedEventBus
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var systemMessageStore: SystemMessageStore
    private lateinit var resourceContextWrapper: ResourceContextWrapper
    private lateinit var commandsStore: LocalCommandsStore
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)


    @Before
    fun setup() {
        localTicketsStore = mockk(relaxed = true, relaxUnitFun = true)
        accountStore = mockk(relaxed = true, relaxUnitFun = true)
        resourceManager = mockk(relaxed = true, relaxUnitFun = true)
        idStore = mockk(relaxed = true, relaxUnitFun = true)
        localCommandsStore = mockk(relaxed = true, relaxUnitFun = true)
        accessDeniedEventBus = mockk(relaxed = true, relaxUnitFun = true)
        systemMessageStore = mockk(relaxed = true, relaxUnitFun = true)
        resourceContextWrapper = mockk(relaxed = true, relaxUnitFun = true)
        commandsStore = mockk(relaxed = true, relaxUnitFun = true)

        val app = RuntimeEnvironment.application
        val preferences: SharedPreferences = app.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
        preferencesManager = PreferencesManager(TEST_APP_ID, preferences)

        synchronizer = TestSynchronizer(
            api = TestServiceDeskApi(testScope),
            localTicketsStore = localTicketsStore,
            accessDeniedEventBus = accessDeniedEventBus,
            accountStore = accountStore,
            resourceManager = resourceManager,
            idStore = idStore,
            commandsStore = localCommandsStore,
            preferences = preferencesManager,
            systemMessageStore = systemMessageStore,
            resourceContextWrapper = resourceContextWrapper,
            testDispatcher = testDispatcher,
        )

        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanData() {
        TestServiceDeskApi.cleanSyncData()
        Dispatchers.resetMain()
        testScope.cancel()
        testDispatcher.cancelChildren()
        synchronizer.close()
    }

    @Test
    fun runSyncEmptyTickets() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(1, TestServiceDeskApi.getSyncCount())
    }

    @Test
    fun trotSimpleGetData() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        val ticketsTry2 = synchronizer.syncData(SyncRequest.Data, true)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 5000L)
    }

    @Test
    fun trotMarkTicketIsRead() = testScope.runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(markTicketIsRead)
        val ticketsTry2 = synchronizer.syncCommand(markTicketIsReadRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 5000L)
    }

    @Test
    fun trotSetPushToken() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(setPushTokenResponse)
        val ticketsTry2 = synchronizer.syncCommand(setPushTokenRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 5000L)
    }

    @Test
    fun trotCreateComment() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ticketsTry2 = synchronizer.syncCommand(createCommentRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 1000L)
    }

    @Test
    fun trotCalcOperatorTimeCommand() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(calcOperatorTime)
        val ticketsTry2 = synchronizer.syncCommand(calcOperatorTimeRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 1000L)
    }

    @Test
    fun syncIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        advanceTimeBy(5000)

        val exception = if (!ticketsTry.isSuccess()) ticketsTry.error else null
        println("exception: $exception")
        synchronizer.close()
        assertEquals(false, ticketsTry.isSuccess())
    }

    @Test
    fun syncFailDelayIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)

        val exception = if (!ticketsTry.isSuccess()) ticketsTry.error else null
        println("exception: $exception")
        synchronizer.close()
        assertEquals(false, ticketsTry.isSuccess())
    }

    /**
     * If it is a SyncRequest.Data min delay is 5 second (due to throttling)
     * Even if fail delay is 1 second
     * p.s. since the throttling timer uses real time, both delay fail delay and throttling delay are used in the test
     */
    @Test
    fun syncMinFailDelayIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)

        synchronizer.syncData(SyncRequest.Data, true)
        advanceTimeBy(6000)

        val syncCount = TestServiceDeskApi.getSyncCount()
        println(syncCount)
        assertEquals(2, syncCount)
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 6000L)
        synchronizer.close()
    }

    @Test
    fun syncMinFailDelayWhenCreateCommentIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)

        val job = testScope.launch {
            synchronizer.syncCommand(createCommentRequest)
        }
        advanceTimeBy(3000L)
        job.cancel()

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(2, syncCount)
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 2000L)
        synchronizer.close()

    }

    /**
     * Since the throttling timer uses real time,
     * both delay fail delay and throttling delay are used in the test (+1s in this case)
     */
    @Test
    fun sync2FailDelayWhenCreateCommentIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)

        val job = testScope.launch {
            synchronizer.syncCommand(createCommentRequest)
        }
        advanceTimeBy(6000L)
        job.cancel()
        synchronizer.close()

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(true, syncCount >= 3)
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        listSyncTime.assertDifferenceAtLeast(0, 1, 2000L)
        listSyncTime.assertDifferenceInRange(1, 2, 2000L..4000L)

    }

    @Test
    fun sync5FailDelayWhenCreateCommentIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)

        val job = testScope.launch {
            synchronizer.syncCommand(createCommentRequest)
        }
        advanceTimeBy(50000L)
        job.cancel()
        synchronizer.close()

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(true, syncCount >= 4)
    }

    @Test
    fun syncWithErrorCode400() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)
        TestServiceDeskApi.setGetTicketsException(HttpException(FAILED_AUTHORIZATION_ERROR_CODE))

        synchronizer.syncData(SyncRequest.Data, true)
        advanceTimeBy(50000L)

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(1, syncCount)
    }

    @Test
    fun syncWithErrorCode403() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)
        TestServiceDeskApi.setGetTicketsException(HttpException(FAILED_AUTHORIZATION_ERROR_CODE_FORBIDDEN))

        synchronizer.syncData(SyncRequest.Data, true)
        advanceTimeBy(50000L)

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(1, syncCount)
    }

    @Test
    fun syncWithErrorCode429() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)
        TestServiceDeskApi.setGetTicketsException(HttpException(FAILED_SYNC_ERROR_CODE))

        preferencesManager.saveLastActiveTime(System.currentTimeMillis())

        synchronizer.syncData(SyncRequest.Data, true)
        advanceTimeBy(50000L)

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(1, syncCount)

        val lastActiveTimeAfter429 = preferencesManager.getLastActiveTime()
        assertEquals(NO_UPDATES, lastActiveTimeAfter429)
    }

    @Test
    fun syncWithErrorInCommand() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createCommentError)

        val ticketsTry = synchronizer.syncCommand(createCommentRequest)
        advanceTimeBy(50000L)

        val syncCount = TestServiceDeskApi.getSyncCount()
        assertEquals(false, ticketsTry.isSuccess())
        assertEquals(1, syncCount)
    }

    fun List<Long>.assertDifferenceAtLeast(index1: Int, index2: Int, min: Long) {
        val diff = this[index2] - this[index1]
        println("Expected time: ≥ $min, actual time: $diff")
        assertTrue("Difference should be at least $min, but was $diff", diff >= min)
    }

    fun List<Long>.assertDifferenceInRange(index1: Int, index2: Int, range: LongRange) {
        val diff = this[index2] - this[index1]
        println("Expected time: $range, actual time: $diff")
        assertTrue("Difference should be in $range, but was $diff", diff in range)
    }

}