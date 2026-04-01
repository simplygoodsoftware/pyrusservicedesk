import InitData.TEST_TICKET_ID
import InitData.calcOperatorTimeResultNotNull
import InitData.calcOperatorTimeResultNull
import InitData.inActiveTicketWithComments
import InitData.ticketWithComments
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeature
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeatureFactory
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComments
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoRefreshFeatureOperatorTimeTest {

    private val storeFactory: StoreFactory = DefaultStoreFactory()

    private lateinit var idStore: IdStore

    private lateinit var systemMessageStore: SystemMessageStore
    private lateinit var repository: SdRepository
    private lateinit var liveUpdates: LiveUpdates
    private lateinit var localTicketsStore: LocalTicketsStore
    private lateinit var preferencesManager: PreferencesManager

    private val listSyncCount = mutableListOf<Long>()

    @Before
    fun setUp() {

        repository = mockk(relaxed = true, relaxUnitFun = true)
        liveUpdates = mockk(relaxed = true, relaxUnitFun = true)
        localTicketsStore = mockk(relaxed = true, relaxUnitFun = true)
        preferencesManager = mockk(relaxed = true)
        idStore = mockk()

        every { idStore.getTicketServerId(any()) } returns TEST_TICKET_ID
        systemMessageStore = SystemMessageStore(idStore)
    }

    @Test
    fun shouldNotExecuteWhenTicketIdIsNull() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)

        coVerify(exactly = 0) { repository.sendCalcOperatorTime(any()) }
        coVerify(exactly = 0) { localTicketsStore.getTicketWithComments(any()) }
        autoRefreshFeatureFactory.cancel()

    }

    @Test
    fun shouldNotExecuteWhenTicketIsInactive() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, inActiveTicketWithComments)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)

        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        checkSendCalcOperatorTimeCount(MILLISECONDS_IN_MINUTE * 2L, 0)
        coVerify(atLeast = 1) { localTicketsStore.getTicketWithComments(any()) }

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    @Test
    fun shouldNotExecuteWhenTicketIsNullFromStore() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, null)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        checkSendCalcOperatorTimeCount(MILLISECONDS_IN_MINUTE * 2L, 0)
        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    @Test
    fun shouldNotExecuteWhenSystemMessageStoreTicketIdIsNull() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, false)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        checkSendCalcOperatorTimeCount(MILLISECONDS_IN_MINUTE * 2L, 0)
        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }


    @Test
    fun shouldSendOperatorTimeWhenTicketStateFlowEmits() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, ticketWithComments)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        advanceTimeBy(MILLISECONDS_IN_MINUTE * 2L)
        runCurrent()
        coVerify(atLeast = 1) { repository.sendCalcOperatorTime(any()) }

        val message =
            systemMessageStore.operatorResponseTimeMessageStateFlow().value
        println(message)
        assertFalse(message.isNullOrBlank())
        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    @Test
    fun shouldSendOperatorTimeEveryMinutes() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, ticketWithComments)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        val checkInterval = MILLISECONDS_IN_MINUTE * 5L
        checkSendCalcOperatorTimeCount(checkInterval, 6)

        val intervals = listSyncCount.toList()
        cancelTest(testDispatcher, autoRefreshFeatureFactory)
        checkAllIntervals(intervals)
    }

    @Test
    fun shouldStopWhenTicketBecomesInactiveDuringLoop() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, ticketWithComments)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        val checkInterval = MILLISECONDS_IN_MINUTE * 3L
        checkSendCalcOperatorTimeCount(checkInterval, 4)

        coEvery {
            localTicketsStore.getTicketWithComments(any())
        } returns inActiveTicketWithComments

        checkSendCalcOperatorTimeCount(checkInterval, 4)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    @Test
    fun shouldStopWhenTicketIdBecomesNullDuringLoop() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, ticketWithComments)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        val checkInterval = MILLISECONDS_IN_MINUTE * 3L
        checkSendCalcOperatorTimeCount(checkInterval, 4)

        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, false)

        checkSendCalcOperatorTimeCount(checkInterval, 4)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * 5 потому что за это время успевает выполниться еще 1 запрос
     */
    @Test
    fun shouldStopWhenOperatorTimeMessageBecomesNullDuringLoop() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(calcOperatorTimeResultNotNull, ticketWithComments)

        val autoRefreshFeatureFactory =
            createAutoRefreshFeature(testDispatcher, this)
        systemMessageStore.setNecessityTimeSystemMessage(TEST_TICKET_ID, true)

        val checkInterval = MILLISECONDS_IN_MINUTE * 3L
        checkSendCalcOperatorTimeCount(checkInterval, 4)

        coEvery { repository.sendCalcOperatorTime(any()) } returns Try.Success(
            calcOperatorTimeResultNull
        )

        checkSendCalcOperatorTimeCount(checkInterval, 5)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    private fun checkAllIntervals(intervals: List<Long>) {
        for (i in 0 until intervals.size - 1) {
            val current = intervals[i]
            val next = intervals[i + 1]

            val difference = next - current
            assertEquals(MILLISECONDS_IN_MINUTE.toLong(), difference)
        }
    }

    private fun TestScope.setupMocks(
        commandResult: TicketCommandResultDto,
        ticketWithComments: TicketWithComments?,
    ) {
        coEvery {
            localTicketsStore.getTicketWithComments(any())
        } returns ticketWithComments
        coEvery { repository.sendCalcOperatorTime(any()) } answers {
            listSyncCount.add(currentTime)
            Try.Success(commandResult)
        }
    }


    private fun createAutoRefreshFeature(
        testDispatcher: TestDispatcher,
        testScope: TestScope,
    ): AutoRefreshFeature {
        return AutoRefreshFeatureFactory(
            storeFactory = storeFactory,
            repository = repository,
            preferencesManager = preferencesManager,
            systemMessageStore = systemMessageStore,
            localTicketsStore = localTicketsStore,
            timeProvider = TestTimeProvider(testScope),
        ).create(liveUpdates, testDispatcher)
    }

    private fun TestScope.checkSendCalcOperatorTimeCount(
        checkInterval: Long,
        expectedCount: Int,
    ) {
        advanceTimeBy(checkInterval)
        runCurrent()
        coVerify(exactly = expectedCount) { repository.sendCalcOperatorTime(any()) }
    }

    private fun cancelTest(
        testDispatcher: TestDispatcher,
        autoRefreshFeatureFactory: AutoRefreshFeature,
    ) {
        autoRefreshFeatureFactory.cancel()
        testDispatcher.cancelChildren()
        listSyncCount.clear()
    }
}