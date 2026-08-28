import com.ibm.icu.util.Calendar
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeature
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeatureFactory
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AutoRefreshFeatureTest {
    private val idStore = IdStore()

    private val systemMessageStore = SystemMessageStore(idStore)
    private lateinit var repository: SdRepository
    private lateinit var liveUpdates: LiveUpdates
    private lateinit var localTicketsStore: LocalTicketsStore
    private lateinit var preferencesManager: PreferencesManager

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var isStartedFlow: MutableStateFlow<Boolean>
    private lateinit var lastActiveTimeFlow: MutableStateFlow<Long>
    private lateinit var sdOpenFlow: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        repository = mockk(relaxed = true, relaxUnitFun = true)
        liveUpdates = mockk(relaxed = true, relaxUnitFun = true)
        localTicketsStore = mockk(relaxed = true, relaxUnitFun = true)
        preferencesManager = mockk(relaxed = true)

    }

    internal fun getTestTicketsUpdateInterval(lastActiveTime: Long): Long {
        val diff = System.currentTimeMillis() - lastActiveTime
        return when {
            diff <= MILLISECONDS_IN_MINUTE -> 5L * MILLISECONDS_IN_SECOND
            diff <= 5 * MILLISECONDS_IN_MINUTE -> 15L * MILLISECONDS_IN_SECOND
            diff <= MILLISECONDS_IN_HOUR -> MILLISECONDS_IN_MINUTE.toLong()
            diff <= 3 * MILLISECONDS_IN_DAY || PyrusServiceDesk.sdIsOpen.value -> 3 * MILLISECONDS_IN_MINUTE.toLong()
            else -> NO_UPDATES
        }
    }

    /**
     * If the interval is changed to NO_UPDATES, the sync call should stop.
     * Interval - 5 secund
     * In 5 second we should call 2 sync (sync -> 5 second delay -> sync)
     */
    @Test
    fun whenIntervalBecomesNOUPDATESShouldStopCallingSync() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(
            lastActiveTime = System.currentTimeMillis(),
            sdkIsOpen = false,
            flowIsStarted = true
        )

        val initialInterval = MILLISECONDS_IN_SECOND * 5L
        every { liveUpdates.getTicketsUpdateInterval(any()) } returns initialInterval

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(initialInterval, 2)

        every { liveUpdates.getTicketsUpdateInterval(any()) } returns NO_UPDATES

        checkSyncCount(initialInterval, 2)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * Test the periodicity of calling sync at a certain interval
     * Interval - 5 secund
     * In 5 second we should call 2 sync (sync -> 5 second delay -> sync)
     */
    @Test
    fun whenInterval5000CountCallingSync() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(
            lastActiveTime = System.currentTimeMillis(),
            sdkIsOpen = false,
            flowIsStarted = true
        )

        val initialInterval = MILLISECONDS_IN_SECOND * 5L
        val checkInterval = MILLISECONDS_IN_SECOND * 20L
        every { liveUpdates.getTicketsUpdateInterval(any()) } returns initialInterval

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 5)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * Test the periodicity of calling sync at a certain interval
     * Interval - 15 secund
     * In 15 second we should call 2 sync (sync -> 15 second delay -> sync)
     */
    @Test
    fun whenInterval15000CountCallingSync() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        setupMocks(
            lastActiveTime = System.currentTimeMillis(),
            sdkIsOpen = false,
            flowIsStarted = true
        )

        val initialInterval = MILLISECONDS_IN_SECOND * 15L
        val checkInterval = MILLISECONDS_IN_SECOND * 15L * 2
        every { liveUpdates.getTicketsUpdateInterval(any()) } returns initialInterval

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * Test the periodicity of calling sync at a certain interval
     * Interval - 1 minute
     * In 1 minute we should call 2 sync (sync -> 1 minute delay -> sync)
     */
    @Test
    fun whenInterval60CountCallingSync() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        setupMocks(
            lastActiveTime = System.currentTimeMillis(),
            sdkIsOpen = false,
            flowIsStarted = true
        )


        val initialInterval = MILLISECONDS_IN_SECOND * 60L
        val checkInterval = MILLISECONDS_IN_SECOND * 60L * 2
        every { liveUpdates.getTicketsUpdateInterval(any()) } returns initialInterval

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }


    /**
     * If servicedesk is open should to call sync
     * Interval - 15 secund
     * In 15 secund we should call 2 sync (sync -> 15 second delay -> sync)
     * sdOpenFlow.value = true
     * isStartedFlow.value = false
     */
    @Test
    fun whenSdIsOpenShouldStartWithInterval() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val threeMinutesAgo = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -3)
        }.timeInMillis

        setupMocks(
            lastActiveTime = threeMinutesAgo,
            sdkIsOpen = true,
            flowIsStarted = false
        )

        setInterval(threeMinutesAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 15L * 2

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If servicedesk is open should to call sync
     * Interval - 15 secund
     * In 15 secund we should call 2 sync (sync -> 15 second delay -> sync)
     * sdOpenFlow.value = true
     * isStartedFlow.value = false
     */
    @Test
    fun whenSdIsOpenShouldStartEvenIfLastActiveTimeIsMoreThen3DaysAgo() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val fourDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -4)
        }.timeInMillis

        setupMocks(
            lastActiveTime = fourDaysAgo,
            sdkIsOpen = true,
            flowIsStarted = false
        )

        setInterval(fourDaysAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 60L * 6

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If servicedesk has been opened, should to call sync
     * Interval - 60 secund
     * In 5 secund we should call 2 sync (sync -> 5 second delay -> sync)
     * sdOpenFlow.value first = false
     * sdOpenFlow.value second = true
     */
    @Test
    fun whenSdIsOpenedShouldStartUpdates() = runTest(testDispatcher, 60.seconds) {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val fourDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -4)
        }.timeInMillis

        setupMocks(
            lastActiveTime = fourDaysAgo,
            sdkIsOpen = false,
            flowIsStarted = false
        )

        setInterval(fourDaysAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 60L * 6

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 0)

        PyrusServiceDesk.updateSdIsOpen(true)

        setInterval(fourDaysAgo)
        checkSyncCount(checkInterval, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If servicedesk has been opened, should to call sync. Even if an update subscription is already active
     * Interval - 15 secund
     * In 15 secund we should call 2 sync (sync -> 15 second delay -> sync)
     * sdOpenFlow.value first = false
     * sdOpenFlow.value second = true
     * isStartedFlow.value = true
     */
    @Test
    fun whenSdIsOpenedShouldStartUpdatesEvenLiveUpdatesIsStarted() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val threeMinutesAgo = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -3)
        }.timeInMillis

        setupMocks(
            lastActiveTime = threeMinutesAgo,
            sdkIsOpen = false,
            flowIsStarted = true
        )

        setInterval(threeMinutesAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 15L * 2 + MILLISECONDS_IN_SECOND * 5L

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        PyrusServiceDesk.updateSdIsOpen(true)

        val checkInterval2 = MILLISECONDS_IN_SECOND * 2L
        checkSyncCount(checkInterval2, 4)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If servicedesk is not open and the update subscription isn't active, don't call Sync.
     * Interval - 60 secund
     * In 60 secund we should call 2 sync (sync -> 60 second delay -> sync)
     * sdOpenFlow.value = false
     * isStartedFlow.value = false
     */
    @Test
    fun whenSdIsOpenedFalseAndLiveUpdatesIsStartedFalseStartWithNOUPDATES() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        setupMocks(
            lastActiveTime = NO_UPDATES,
            sdkIsOpen = false,
            flowIsStarted = false
        )

        setInterval(NO_UPDATES)
        val checkInterval = MILLISECONDS_IN_SECOND * 60L * 2

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 0)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If the SdIsOpen flag was true and was set to true again, we don't trigger the auto-update.
     * (test the distinctUntilChanged)
     * Interval - 15 secund
     * In 15 secund we should call 2 sync (sync -> 15 second delay -> sync)
     * sdOpenFlow.value = true
     * isStartedFlow.value = false
     */
    @Test
    fun whenSdIsOpenSecondNotShouldStartNewInterval() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val threeMinutesAgo = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -3)
        }.timeInMillis

        setupMocks(
            lastActiveTime = threeMinutesAgo,
            sdkIsOpen = true,
            flowIsStarted = false
        )


        setInterval(threeMinutesAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 15L * 2 + MILLISECONDS_IN_SECOND * 5L

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        PyrusServiceDesk.updateSdIsOpen(true)
        val checkInterval2 = MILLISECONDS_IN_SECOND * 2L
        checkSyncCount(checkInterval2, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If LiveUpdates has been started, should to call sync
     * Interval - 15 secund
     * In 15 secund we should call 2 sync (sync -> 15 second delay -> sync)
     * isStartedFlow.value first = false
     * isStartedFlow.value second = true
     */
    @Test
    fun whenLiveUpdatesIsStartedShouldStartUpdates() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val threeMinutesAgo = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -3)
        }.timeInMillis

        setupMocks(
            lastActiveTime = threeMinutesAgo,
            sdkIsOpen = false,
            flowIsStarted = false
        )

        setInterval(threeMinutesAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 15L * 2

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 0)

        isStartedFlow.value = true
        checkSyncCount(checkInterval, 3)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }


    /**
     * If the time of the last activity has changed, we start a new cycle with a new interval
     * lastActiveTimeFlow.value first = 3 minutes ago
     * astActiveTimeFlow.value second = now
     * In 5 secund we should call 2 sync (sync -> 5 second delay -> sync)
     */
    @Test
    fun whenLastActiveTimeChangedShouldStartWithNewInterval() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val threeMinutesAgo = Calendar.getInstance().apply {
            add(Calendar.MINUTE, -3)
        }.timeInMillis
        setupMocks(
            lastActiveTime = threeMinutesAgo,
            sdkIsOpen = false,
            flowIsStarted = true
        )

        setInterval(threeMinutesAgo)
        val checkInterval = MILLISECONDS_IN_SECOND * 15L * 2

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 3)

        val newLastActiveTime = System.currentTimeMillis()
        setInterval(newLastActiveTime)
        lastActiveTimeFlow.value = newLastActiveTime
        checkSyncCount(MILLISECONDS_IN_SECOND * 15L, 7)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    /**
     * If the interval has changed, but neither isStarted nor lastActiveTime, nor SdIsOpen have changed, we still use the new interval
     * Interval first - 5 secund
     * Interval second - 15 secund
     * In 5 secund we should call 2 sync (sync -> 5 second delay -> sync)
     */
    @Test
    fun updatesAfterAMinute() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val calendar = Calendar.getInstance()
        setupMocks(
            lastActiveTime = calendar.timeInMillis,
            sdkIsOpen = false,
            flowIsStarted = true
        )

        var interval = MILLISECONDS_IN_SECOND * 5L
        every {
            liveUpdates.getTicketsUpdateInterval(any())
        } returns interval

        val checkInterval = MILLISECONDS_IN_SECOND * 30L

        val autoRefreshFeatureFactory = createAutoRefreshFeature(testDispatcher, this)

        checkSyncCount(checkInterval, 7)

        interval = MILLISECONDS_IN_SECOND * 15L
        every {
            liveUpdates.getTicketsUpdateInterval(any())
        } returns interval

        checkSyncCount(MILLISECONDS_IN_SECOND * 15L, 8)

        cancelTest(testDispatcher, autoRefreshFeatureFactory)
    }

    private fun setupMocks(lastActiveTime: Long, sdkIsOpen: Boolean, flowIsStarted: Boolean) {
        clearAllMocks()
        unmockkAll()

        isStartedFlow = MutableStateFlow(flowIsStarted)
        lastActiveTimeFlow = MutableStateFlow(lastActiveTime)

        coEvery { liveUpdates.isStartedFlow() } returns isStartedFlow
        coEvery { preferencesManager.getLastActiveTimeFlow() } returns lastActiveTimeFlow
        PyrusServiceDesk.updateSdIsOpen(sdkIsOpen)
    }

    private fun createAutoRefreshFeature(testDispatcher: TestDispatcher, testScope: TestScope): AutoRefreshFeature {
        val storeFactory = DefaultStoreFactory(testDispatcher, testDispatcher)
        return AutoRefreshFeatureFactory(
            storeFactory = storeFactory,
            repository = repository,
            preferencesManager = preferencesManager,
            systemMessageStore = systemMessageStore,
            localTicketsStore = localTicketsStore,
            timeProvider = TestTimeProvider(testScope),
        ).create(liveUpdates)
    }

    private fun cancelTest(testDispatcher: TestDispatcher, autoRefreshFeatureFactory: AutoRefreshFeature) {
        autoRefreshFeatureFactory.cancel()
        testDispatcher.cancelChildren()
        testDispatcher.cancel()
    }

    private fun TestScope.checkSyncCount(checkInterval: Long, expectedCount: Int) {
        advanceTimeBy(checkInterval)
        runCurrent()
        coVerify(exactly = expectedCount) { repository.sync() }
    }

    private fun setInterval(lastActiveTime: Long) {
        val interval = getTestTicketsUpdateInterval(lastActiveTime)
        every {
            liveUpdates.getTicketsUpdateInterval(any())
        } returns interval
    }

    companion object {
        private const val NO_UPDATES = -1L
    }
}
