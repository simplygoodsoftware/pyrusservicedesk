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
import android.net.Uri
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk.SdConstants.PYRUS_BASE_DOMAIN
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.RepositoryMapper
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.LocalAttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.UploadCancelledException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CompletableDeferred
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
import java.io.File

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
    private lateinit var repositoryMapper: RepositoryMapper
    private lateinit var remoteFileStore: RemoteFileStore

    private lateinit var fileResolver: FileResolver

    /** Local ids that have been removed from the store, see LocalCommandsStore.hasCommand. */
    private val removedCommandIds = mutableSetOf<Long>()

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
        fileResolver = mockk()
        remoteFileStore = mockk()

        every { localCommandsStore.hasCommand(any()) } answers { firstArg<Long>() !in removedCommandIds }
        every { localCommandsStore.removeCommand(any<Long>()) } answers { removedCommandIds += firstArg<Long>() }

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

    /**
     * The comment that has been cancelled must not be returned to the store by the uploading that
     * is still being finished, otherwise it hangs in the uploading state forever.
     */
    @Test
    fun shouldNotSaveCommandAfterCancelUploadFile() = runTest(testDispatcher) {
        setupMocks(this)
        val savedCommands = setupAttachmentMocks()
        val finishUpload = CompletableDeferred<Try<FileUploadResponseData>>()
        coEvery { remoteFileStore.uploadFile(any(), any(), any()) } coAnswers {
            thirdArg<(Int) -> Unit>().invoke(TEST_PROGRESS)
            finishUpload.await()
        }

        repository.addAttachComment(userInternalV1, TEST_TICKET_ID, TEST_FILE_URI)
        advanceUntilIdle()

        assertTrue("the progress must be saved while the file is uploading", savedCommands.isNotEmpty())
        val savedBeforeCancel = savedCommands.size

        repository.cancelUploadFile(TEST_ATTACH_COMMAND_LOCAL_ID, TEST_ATTACHMENT_ID)
        finishUpload.complete(Try.Failure(UploadCancelledException()))
        advanceUntilIdle()

        verify { localCommandsStore.removeCommand(TEST_ATTACH_COMMAND_LOCAL_ID) }
        verify { remoteFileStore.cancelUploading(any()) }
        assertEquals(
            "the cancelled command must not be saved again",
            savedBeforeCancel,
            savedCommands.size,
        )
    }

    /**
     * The pending commands are sent one by one on the start, so a command can be cancelled while
     * it is still waiting in the queue. The sending that reaches it later must skip it.
     */
    @Test
    fun shouldNotSendCommandRemovedWhileItWaitsInTheQueue() = runTest(testDispatcher) {
        TestServiceDeskApi.setGetTicketsResponse(Responses.emptyTickets)
        val firstUpload = CompletableDeferred<Try<FileUploadResponseData>>()
        every { localCommandsStore.getCommands() } returns listOf(
            attachmentCommandEntity(),
            attachmentCommandEntity(
                localId = QUEUED_ATTACH_COMMAND_LOCAL_ID,
                commandId = QUEUED_ATTACH_COMMAND_ID,
                attachmentId = QUEUED_ATTACHMENT_ID,
                fileName = QUEUED_FILE_NAME,
                fileUri = QUEUED_FILE_URI,
            ),
        )
        coEvery { remoteFileStore.uploadFile(any(), any(), any()) } coAnswers {
            if (firstArg<File>().name == TEST_FILE_NAME) firstUpload.await()
            else Try.Success(FileUploadResponseData(TEST_GUID, null))
        }
        setupMocks(this)
        setupAttachmentMocks()
        advanceUntilIdle()

        // the first command is uploading, the second one is still waiting in the queue
        repository.cancelUploadFile(QUEUED_ATTACH_COMMAND_LOCAL_ID, QUEUED_ATTACHMENT_ID)
        firstUpload.complete(Try.Success(FileUploadResponseData(TEST_GUID, null)))
        advanceUntilIdle()

        coVerify(exactly = 1) { remoteFileStore.uploadFile(any(), any(), any()) }
    }

    /**
     * The comment can be cancelled when its files are already uploaded and the command is being
     * synced. A failed sync must not return such a command to the store as an error one, otherwise
     * the cancelled comment appears in the chat again.
     */
    @Test
    fun shouldNotSaveCommandCancelledWhileSyncing() = runTest(testDispatcher) {
        TestServiceDeskApi.setGetTicketsResponse(Responses.emptyTickets)
        setupMocks(this)
        val savedCommands = setupAttachmentMocks()
        val finishUpload = CompletableDeferred<Try<FileUploadResponseData>>()
        coEvery { remoteFileStore.uploadFile(any(), any(), any()) } coAnswers { finishUpload.await() }

        repository.addAttachComment(userInternalV1, TEST_TICKET_ID, TEST_FILE_URI)
        advanceUntilIdle()

        // the file is uploaded, the command is passed to the synchronizer
        finishUpload.complete(Try.Success(FileUploadResponseData(TEST_GUID, null)))
        testDispatcher.scheduler.runCurrent()
        val savedBeforeCancel = savedCommands.size

        repository.cancelUploadFile(TEST_ATTACH_COMMAND_LOCAL_ID, TEST_ATTACHMENT_ID)
        advanceUntilIdle()

        verify { localCommandsStore.removeCommand(TEST_ATTACH_COMMAND_LOCAL_ID) }
        assertEquals(
            "the command cancelled while syncing must not be saved as an error one",
            savedBeforeCancel,
            savedCommands.size,
        )
    }

    @Test
    fun shouldSaveGuidOfUploadedAttachment() = runTest(testDispatcher) {
        TestServiceDeskApi.setGetTicketsResponse(createComment)
        setupMocks(this)
        val savedCommands = setupAttachmentMocks()
        coEvery { remoteFileStore.uploadFile(any(), any(), any()) } coAnswers {
            thirdArg<(Int) -> Unit>().invoke(TEST_PROGRESS)
            Try.Success(FileUploadResponseData(TEST_GUID, null))
        }

        repository.addAttachComment(userInternalV1, TEST_TICKET_ID, TEST_FILE_URI)
        advanceUntilIdle()

        assertTrue(
            "the guid of the uploaded file must be saved",
            savedCommands.any { it.attachments?.firstOrNull()?.guid == TEST_GUID },
        )
    }

    /**
     * Mocks the creation of a comment with one not uploaded attachment and returns the list of the
     * commands that have been saved to the store.
     */
    private fun setupAttachmentMocks(): List<CommandWithAttachmentsEntity> {
        val savedCommands = mutableListOf<CommandWithAttachmentsEntity>()

        every { fileResolver.getFileData(any()) } returns FileData(
            fileName = TEST_FILE_NAME,
            bytesSize = TEST_FILE_SIZE,
            uri = TEST_FILE_URI,
            isLocal = true,
        )
        every {
            localCommandsStore.addAttachmentCommand(any(), any(), any(), any())
        } returns attachmentCommandEntity()
        every { localCommandsStore.addOrUpdatePendingCommand(capture(savedCommands)) } just Runs
        every { remoteFileStore.cancelUploading(any<UploadFileHook>()) } just Runs

        return savedCommands
    }

    private fun attachmentCommandEntity(
        localId: Long = TEST_ATTACH_COMMAND_LOCAL_ID,
        commandId: String = TEST_ATTACH_COMMAND_ID,
        attachmentId: Long = TEST_ATTACHMENT_ID,
        fileName: String = TEST_FILE_NAME,
        fileUri: Uri = TEST_FILE_URI,
    ) = CommandWithAttachmentsEntity(
        command = CommandEntity(
            isError = false,
            localId = localId,
            commandId = commandId,
            commandType = TicketCommandType.CreateComment.ordinal,
            userId = null,
            appId = TEST_APP_ID,
            creationTime = System.currentTimeMillis(),
            requestNewTicket = false,
            comment = null,
            ticketId = TEST_TICKET_ID,
            rating = null,
            commentId = null,
            token = null,
            tokenType = null,
            ratingComment = null,
            extraFields = null,
        ),
        attachments = listOf(
            LocalAttachmentEntity(
                id = attachmentId,
                commandId = commandId,
                name = fileName,
                guid = null,
                bytesSize = TEST_FILE_SIZE,
                uri = fileUri.toString(),
            )
        ),
    )

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

    private companion object {
        const val TEST_ATTACH_COMMAND_LOCAL_ID = -10L
        const val TEST_ATTACH_COMMAND_ID = "8e2c0a24-3b1f-4a7c-9d55-6f0a1c2b3d4e"
        const val TEST_ATTACHMENT_ID = 100L
        const val TEST_FILE_NAME = "test.txt"
        const val TEST_FILE_SIZE = 2048
        const val TEST_PROGRESS = 50
        const val TEST_GUID = "test-guid"
        val TEST_FILE_URI: Uri = Uri.parse("file:///tmp/test.txt")

        const val QUEUED_ATTACH_COMMAND_LOCAL_ID = -11L
        const val QUEUED_ATTACH_COMMAND_ID = "1f7b6d90-2c34-4e58-8a11-9b0c5d3e7f22"
        const val QUEUED_ATTACHMENT_ID = 101L
        const val QUEUED_FILE_NAME = "queued.txt"
        val QUEUED_FILE_URI: Uri = Uri.parse("file:///tmp/queued.txt")
    }
}