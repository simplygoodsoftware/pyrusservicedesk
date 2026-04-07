import InitData.TEST_APP_ID
import InitData.TEST_INSTANCE_ID
import InitData.TEST_TICKET_ID
import InitData.firstCommand
import InitData.singleTicket
import InitData.ticket
import InitData.ticketEntity
import InitData.ticketEntity2
import InitData.ticketWithComments
import InitData.ticketWithComments2
import InitData.userInternalV1
import Responses.createComment
import android.content.Context
import android.content.SharedPreferences
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk.SdConstants.PYRUS_BASE_DOMAIN
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.ResourceContextWrapper
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.RepositoryMapper
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
class SdRepositoryTest {

    private lateinit var repository: SdRepository
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
    private lateinit var repositoryMapper: RepositoryMapper
    private lateinit var remoteFileStore: RemoteFileStore

    private lateinit var fileResolver: FileResolver

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)


    @Before
    fun setup() {
        localTicketsStore = mockk(relaxed = true, relaxUnitFun = true)
        resourceManager = mockk(relaxed = true, relaxUnitFun = true)
        idStore = mockk(relaxed = true, relaxUnitFun = true)
        localCommandsStore = mockk(relaxed = true, relaxUnitFun = true)
        accessDeniedEventBus = mockk(relaxed = true, relaxUnitFun = true)
        systemMessageStore = mockk(relaxed = true, relaxUnitFun = true)
        resourceContextWrapper = mockk(relaxed = true, relaxUnitFun = true)
        fileResolver = mockk()
        remoteFileStore = mockk()

        accountStore = AccountStore(
            Account.V1(
                PYRUS_BASE_DOMAIN,
                TEST_INSTANCE_ID,
                TEST_APP_ID
            )
        )
        repositoryMapper = RepositoryMapper(idStore)

        val app = RuntimeEnvironment.application
        val preferences: SharedPreferences = app.getSharedPreferences(
            PREFERENCE_KEY,
            Context.MODE_PRIVATE
        )
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


        coEvery {
            localTicketsStore
                .getTickets()
                .lastOrNull()
                ?.ticketId
        } returns TEST_TICKET_ID

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
    fun shouldSendAndRemoveInitCommands() = runTest(testDispatcher) {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        setupMocks(this)
        coEvery { localCommandsStore.getCommands() } returns listOf(
            CommandWithAttachmentsEntity(
                command = CommandEntity(
                    isError = false,
                    localId = -1,
                    commandId = "b5c9c6b0-5584-4f6b-a8c4-0017998db26e",
                    commandType = TicketCommandType.CreateComment.ordinal,
                    userId = null,
                    appId = TEST_APP_ID,
                    creationTime = System.currentTimeMillis(),
                    requestNewTicket = false,
                    comment = "testComment",
                    ticketId = TEST_TICKET_ID,
                    rating = null,
                    commentId = null,
                    token = null,
                    tokenType = null,
                    ratingComment = null,
                    extraFields = null
                ),
                attachments = emptyList()
            )
        )
        advanceUntilIdle()

        coVerify { localCommandsStore.removeCommand(any<String>()) }

    }

    @Test
    fun notShouldSyncWhenForceIsFalse() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        setupMocks(this)
        val expectedTicket = ticket

        val result = repository.getFeed(
            userInternalV1.userId,
            TEST_TICKET_ID,
            force = false
        )

        advanceUntilIdle()

        assertEquals(0, TestServiceDeskApi.getSyncCount())

        assertTrue(result.isSuccess())
        if (result.isSuccess())
            assertEquals(expectedTicket, result.value)
    }

    @Test
    fun shouldSyncWhenForceIsTrue() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        setupMocks(this)
        val expectedTicket = ticket
        val result = repository.getFeed(
            userInternalV1.userId,
            TEST_TICKET_ID,
            force = true
        )

        advanceUntilIdle()

        assertTrue(result.isSuccess())
        if (result.isSuccess())
            assertEquals(expectedTicket, result.value)
        assertEquals(1, TestServiceDeskApi.getSyncCount())
    }

    @Test
    fun shouldReturnTicketFromGetFeed() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        setupMocks(this)
        val expectedTicket = ticket

        val result = repository.getFeed(userInternalV1.userId, -1, force = true)

        assertTrue(result.isSuccess())
        if (result.isSuccess())
            assertEquals(expectedTicket, result.value)
        assertEquals(0, TestServiceDeskApi.getSyncCount())
    }

    @Test
    fun shouldNotReturnTicketGromGetFeedIfError() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(null)
        setupMocks(this)

        val result = repository.getFeed(
            userInternalV1.userId,
            TEST_TICKET_ID,
            force = true
        )
        synchronizer.close()
        assertFalse(result.isSuccess())
    }

    @Test
    fun shouldEmitGetFeedWhenIdChanges() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ids = MutableStateFlow(0L)
        val results = mutableListOf<FullTicket?>()
        setupMocks(this)
        val ticketFlow = MutableStateFlow(null)
        val commandsFlow: Flow<List<CommandWithAttachmentsEntity>> =
            MutableStateFlow(emptyList())
        every {
            localTicketsStore.getTicketWithCommentsFlow(any())
        } returns ticketFlow
        every { localCommandsStore.getCommandsFlow(any()) } returns commandsFlow

        ids.emit(TEST_TICKET_ID)
        val job = launch {
            repository.getFeedFlowByTicketIdFlow(userInternalV1, ids)
                .collect {
                    results.add(it)
                }
        }

        ids.value = TEST_TICKET_ID + 1
        advanceUntilIdle()
        ids.value = TEST_TICKET_ID + 2
        advanceUntilIdle()
        ids.value = TEST_TICKET_ID + 2
        advanceUntilIdle()
        ids.value = TEST_TICKET_ID + 3
        advanceUntilIdle()

        assertEquals(3, results.size)


        job.cancel()
    }

    @Test
    fun testTicketEntityIsNull() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ids = MutableStateFlow(0L)
        var result: FullTicket? = null
        setupMocks(this)
        val ticketFlow = MutableStateFlow(null)
        val commandsFlow: Flow<List<CommandWithAttachmentsEntity>> =
            MutableStateFlow(emptyList())
        every {
            localTicketsStore.getTicketWithCommentsFlow(any())
        } returns ticketFlow
        every { localCommandsStore.getCommandsFlow(any()) } returns commandsFlow


        ids.emit(TEST_TICKET_ID)
        val job = launch {
            repository.getFeedFlowByTicketIdFlow(userInternalV1, ids)
                .collect {
                    result = it
                }
        }
        advanceUntilIdle()
        val emptyTicket = FullTicket(
            subject = "",
            comments = emptyList(),
            showRating = false,
            showRatingText = null,
            userId = userInternalV1.userId,
            ticketId = TEST_TICKET_ID,
            orgLogoUrl = null,
            isActive = true,
            isRead = true,
            ratingSettings = null,
            welcomeMessage = null,
            operatorTimeMessage = null,
        )

        assertEquals(emptyTicket, result)


        job.cancel()
    }

    @Test
    fun testTicketEntityIsNullWithFirstCommand() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ids = MutableStateFlow(0L)
        var result: FullTicket? = null
        setupMocks(this)
        val ticketFlow = MutableStateFlow(null)
        val commandsFlow: Flow<List<CommandWithAttachmentsEntity>> =
            MutableStateFlow(listOf(firstCommand))
        every {
            localTicketsStore.getTicketWithCommentsFlow(any())
        } returns ticketFlow
        every { localCommandsStore.getCommandsFlow(any()) } returns commandsFlow


        ids.emit(TEST_TICKET_ID)
        val job = launch {
            repository.getFeedFlowByTicketIdFlow(userInternalV1, ids)
                .collect {
                    result = it
                }
        }
        advanceUntilIdle()
        val expectedTicket = FullTicket(
            subject = "test",
            comments = listOf(
                Comment(
                    id = -3,
                    persistentId = -3,
                    isLocal = true,
                    body = "test",
                    isInbound = true,
                    isSupport = false,
                    attachments = null,
                    creationTime = 1775538530550,
                    rating = null,
                    author = null,
                    isSending = true,
                    isSystem = false,
                    systemCommentType = 0
                )
            ),
            showRating = false,
            showRatingText = null,
            ratingSettings = null,
            orgLogoUrl = null,
            userId = TEST_INSTANCE_ID,
            ticketId = -1,
            isActive = true,
            isRead = true,
            welcomeMessage = null,
            operatorTimeMessage = null
        )
        assertEquals(expectedTicket, result)


        job.cancel()
    }

    @Test
    fun testTicketEntityIsNotNull() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ids = MutableStateFlow(0L)
        var result: FullTicket? = null
        setupMocks(this)
        val ticketFlow = MutableStateFlow(ticketWithComments)
        val commandsFlow: Flow<List<CommandWithAttachmentsEntity>> =
            MutableStateFlow(emptyList())
        every {
            localTicketsStore.getTicketWithCommentsFlow(any())
        } returns ticketFlow
        every { localCommandsStore.getCommandsFlow(any()) } returns commandsFlow


        ids.emit(TEST_TICKET_ID)
        val job = launch {
            repository.getFeedFlowByTicketIdFlow(userInternalV1, ids)
                .collect {
                    result = it
                }
        }
        advanceUntilIdle()

        assertEquals(ticket, result)


        job.cancel()
    }

    @Test
    fun test2Tickets() = runTest {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        val ids = MutableStateFlow(0L)
        var result: FullTicket? = null
        setupMocks(this)
        val ticketFlow = MutableStateFlow(ticketWithComments)
        val commandsFlow: Flow<List<CommandWithAttachmentsEntity>> =
            MutableStateFlow(emptyList())
        every {
            localTicketsStore.getTicketWithCommentsFlow(any())
        } returns ticketFlow
        every { localCommandsStore.getCommandsFlow(any()) } returns commandsFlow
        every { localTicketsStore.getTicketsWithComments() } returns listOf(
            ticketWithComments,
            ticketWithComments2
        )
        every { localTicketsStore.getTickets() } returns listOf(
            ticketEntity,
            ticketEntity2
        )
        every { localTicketsStore.getTicketWithComments(any()) } returns ticketWithComments2


        ids.emit(TEST_TICKET_ID + 1)
        val job = launch {
            repository.getFeedFlowByTicketIdFlow(userInternalV1, ids)
                .collect {
                    result = it
                }
        }
        advanceUntilIdle()

        assertEquals(singleTicket, result)
        job.cancel()
    }

    private fun setupMocks(coroutineScope: CoroutineScope) {
        val applicationFlow = MutableStateFlow(
            listOf(
                ApplicationEntity(
                    appId = TEST_APP_ID,
                    orgName = "TEST",
                    orgLogoUrl = null,
                    orgDescription = "aboutOrg",
                    ratingSettings = null,
                    welcomeMessage = null
                )
            )
        )

        repository = SdRepository(
            commandsStore = localCommandsStore,
            repositoryMapper = repositoryMapper,
            fileResolver = fileResolver,
            remoteFileStore = remoteFileStore,
            synchronizer = synchronizer,
            ticketsStore = localTicketsStore,
            coroutineScope = coroutineScope,
            accountStore = accountStore,
            idStore = idStore,
            systemMessageStore = systemMessageStore,
            testDispatcher
        )
        every { systemMessageStore.operatorResponseTimeMessageStateFlow().value } returns null

        every { idStore.getTicketServerId(any()) } returns null
        every { localTicketsStore.getTickets() } returns listOf(ticketEntity)
        every {
            localTicketsStore.getTicketWithComments(any())
        } returns ticketWithComments
        every { localTicketsStore.getTicketsWithComments() } returns listOf(
            ticketWithComments
        )
        every { localTicketsStore.getApplicationsFlow() } returns applicationFlow

    }
}