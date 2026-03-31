import InitData.TEST_TICKET_ID
import InitData.calcOperatorTimeRequest
import InitData.createCommentRequest
import InitData.markTicketIsReadRequest
import InitData.setPushTokenRequest
import Responses.calcOperatorTime
import Responses.createComment
import Responses.emptyTickets
import Responses.markTicketIsRead
import Responses.setPushTokenResponse
import com.pyrus.pyrusservicedesk.AppResourceManager
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
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

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
        preferencesManager = mockk(relaxed = true, relaxUnitFun = true)
        systemMessageStore = mockk(relaxed = true, relaxUnitFun = true)
        resourceContextWrapper = mockk(relaxed = true, relaxUnitFun = true)
        commandsStore = mockk(relaxed = true, relaxUnitFun = true)

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
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true) //TODO kate добавить тест для cancelTimeout
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(1, TestServiceDeskApi.getSyncCount())
    }

    @Test
    fun trotSimpleGetData() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        val ticketsTry2 = synchronizer.syncData(SyncRequest.Data, true)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        val diffTime = listSyncTime[1] - listSyncTime[0]
        println("expected time: >= 5000L, actual time: $diffTime")
        assertEquals(true, diffTime >= 5000L)
    }

    @Test
    fun trotMarkTicketIsRead() = testScope.runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(markTicketIsRead)
        val ticketsTry2 = synchronizer.syncCommand(markTicketIsReadRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        val diffTime = listSyncTime[1] - listSyncTime[0]
        println("expected time: >= 5000L, actual time: $diffTime")
        assertEquals(true, diffTime >= 5000L)
    }

    @Test
    fun trotSetPushToken() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(setPushTokenResponse)
        val ticketsTry2 = synchronizer.syncCommand(setPushTokenRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        val diffTime = listSyncTime[1] - listSyncTime[0]
        println("expected time: >= 5000L, actual time: $diffTime")
        assertEquals(true, diffTime >= 5000L)
    }

    @Test
    fun trotCreateComment() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ticketsTry2 = synchronizer.syncCommand(createCommentRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        val diffTime = listSyncTime[1] - listSyncTime[0]
        println("expected time: >= 1000L, actual time: $diffTime")
        assertEquals(true, diffTime >= 1000L)
    }

    @Test
    fun trotCalcOperatorTimeCommand() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(emptyTickets)
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)
        TestServiceDeskApi.setGetTicketsResponse(calcOperatorTime)
        val ticketsTry2 = synchronizer.syncCommand(calcOperatorTimeRequest)
        assertEquals(true, ticketsTry.isSuccess())
        assertEquals(true, ticketsTry2.isSuccess())
        assertEquals(2, TestServiceDeskApi.getSyncCount())
        val listSyncTime = TestServiceDeskApi.getListSyncTime()
        val diffTime = listSyncTime[1] - listSyncTime[0]
        println("expected time: >= 1000L, actual time: $diffTime")
        assertEquals(true, diffTime >= 1000L)
    }

    @Test
    fun syncIfNoConnection() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

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
        coEvery { localTicketsStore.getTickets().lastOrNull()?.ticketId } returns TEST_TICKET_ID

        val ticketsTry = synchronizer.syncData(SyncRequest.Data, true)

        val exception = if (!ticketsTry.isSuccess()) ticketsTry.error else null
        println("exception: $exception")
        synchronizer.close()
        assertEquals(false, ticketsTry.isSuccess())
    }
}