import InitData.TEST_APP_ID
import InitData.TEST_INSTANCE_ID
import InitData.TEST_TICKET_ID
import InitData.ticket
import InitData.ticketEntity
import InitData.ticketWithComments
import InitData.userInternalV1
import android.content.Context
import android.content.SharedPreferences
import androidx.core.net.toUri
import com.pyrus.pyrusservicedesk.SdConstants.PYRUS_BASE_DOMAIN
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.RecordState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeature
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record.AudioRecordControllerFactory
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.Try2
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.record.AudioRecordController
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer.Companion.NO_UPDATES
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import utils.testComponent
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TicketFeatureFactoryTest {

    private lateinit var accountStore: AccountStore

    private val storeFactory: StoreFactory = DefaultStoreFactory()
    private lateinit var repository: SdRepository
    private lateinit var draftRepository: DraftRepository
    private lateinit var router: PyrusRouter
    private lateinit var fileManager: FileManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var audioRecordControllerFactory: AudioRecordControllerFactory
    private lateinit var audioWrapper: AudioWrapper
    private lateinit var localTicketsStore: LocalTicketsStore
    private lateinit var commandsStore: LocalCommandsStore

    private lateinit var audioRecordController: AudioRecordController
    private val idStore = IdStore()
    private val systemMessageStore = SystemMessageStore(idStore)
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()


    @Before
    fun setUp() {

        repository = mockk(relaxed = true, relaxUnitFun = true)
        localTicketsStore = mockk(relaxed = true, relaxUnitFun = true)
        draftRepository = mockk(relaxed = true, relaxUnitFun = true)
        router = mockk(relaxed = true, relaxUnitFun = true)
        fileManager = mockk(relaxed = true, relaxUnitFun = true)
        audioRecordControllerFactory = mockk(relaxed = true, relaxUnitFun = true)
        audioWrapper = mockk(relaxed = true, relaxUnitFun = true)
        localTicketsStore = mockk(relaxed = true, relaxUnitFun = true)
        commandsStore = mockk(relaxed = true, relaxUnitFun = true)
        audioRecordController = mockk(relaxed = true)
        accountStore = AccountStore(Account.V1(PYRUS_BASE_DOMAIN, TEST_INSTANCE_ID, TEST_APP_ID))
        val app = RuntimeEnvironment.application
        val preferences: SharedPreferences = app.getSharedPreferences(
            PREFERENCE_KEY,
            Context.MODE_PRIVATE
        )
        preferencesManager = PreferencesManager(TEST_APP_ID, preferences)
        preferencesManager.saveLastActiveTime(NO_UPDATES)
        every { audioRecordControllerFactory.create() } returns audioRecordController

        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanData() {
        Dispatchers.resetMain()
    }

    @Test
    fun shouldUpdateStateWhenInputTextChanged() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnMessageChanged("test text from input"))
            println(testResult.models.size)
            println((testResult.models.firstOrNull() as? State.Content)?.inputText)
            println((testResult.models.lastOrNull() as? State.Content)?.inputText)

            println(testResult.models)
            assertEquals(2, testResult.models.size)
            assertTrue((testResult.models.firstOrNull() as? State.Content)?.inputText.isNullOrBlank())
            assertEquals("test text from input", (testResult.models.lastOrNull() as? State.Content)?.inputText)
        }

        feature.cancel()
    }

    @Test
    fun shouldSendCommentWhenClickOnButtonWithText() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnButtonClick("test text from button"))

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 1) { repository.addTextComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES != time)
        }

        feature.cancel()
    }

    @Test
    fun shouldSendCommentWhenClickOnButtonWithEmptyText() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnButtonClick("  "))

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 0) { repository.addTextComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
        feature.cancel()
    }

    @Test
    fun shouldSendCommentWhenOnRatingClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnRatingClick(5, null))

            println(testResult.models)
            assertEquals(2, testResult.models.size)
            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.ticket?.showRating == false)
            coVerify(exactly = 1) { repository.addRatingComment(any(), any(), any(), any()) }

            val effect = testResult.effects.firstOrNull()
            assertEquals(
                TicketContract.Effect.Outer.OpenRatingComment(
                    rateUsText = null
                ), effect
            )
            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES != time)
        }
        feature.cancel()
    }

    @Test
    fun shouldSendCommentWhenOnRatingTextClick() {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnRatingClick(null, "rating text"))

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 1) { repository.addRatingComment(any(), any(), any(), any()) }

            assertEquals(0, testResult.effects.size)
            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES != time)
        }
        feature.cancel()
    }

    @Test
    fun notShouldSendCommentIfRatingIsNull() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnRatingClick(null, null))

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 0) { repository.addRatingComment(any(), any(), any(), any()) }
            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
        feature.cancel()
    }

    @Test
    fun shouldOpenErrorCommentDialogWhenOnErrorCommentClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnErrorCommentClick(1))

            assertEquals(2, testResult.models.size)
            val effect = testResult.effects.firstOrNull()
            assertTrue(effect is TicketContract.Effect.Outer.ShowErrorCommentDialog)
            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
            feature.cancel()
        }
    }

    @Test
    fun shouldSendCommentWhenOnSendClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnMessageChanged("test text from input"))

            dispatch(Message.Outer.OnSendClick)

            println(testResult.models)
            assertEquals(3, testResult.models.size)
            coVerify(exactly = 1) { repository.addTextComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES != time)
        }
        feature.cancel()
    }

    @Test
    fun notShouldSendCommentWhenOnSendClickEmptyText() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnSendClick)

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 0) { repository.addTextComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
        feature.cancel()
    }

    @Test
    fun shouldShowAttachVariantsWhenOnShowAttachVariantsClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnShowAttachVariantsClick)


            println(testResult.models)
            assertEquals(2, testResult.models.size)
            val effect = testResult.effects.firstOrNull()
            assertTrue(effect is TicketContract.Effect.Outer.ShowAttachVariants)

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
        feature.cancel()
    }


    @Test
    fun shouldRefreshWhenOnRefresh() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnRefresh)

            assertEquals(2, testResult.models.size)
            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.isLoading == true)
            coVerify(exactly = 2) { repository.getFeed(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
        feature.cancel()
    }

    @Test
    fun notShouldRefreshIfIsLoading() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnRefresh)
            dispatch(Message.Outer.OnRefresh)

            assertEquals(3, testResult.models.size)
            coVerify(exactly = 2) { repository.getFeed(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnCancelUploadClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnCancelUploadClick(1, 1))


            println(testResult.models)
            assertEquals(2, testResult.models.size)
            coVerify(exactly = 1) { repository.cancelUploadFile(any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnInfoClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnInfoClick)

            println(testResult.models)
            assertEquals(2, testResult.models.size)
            val effect = testResult.effects.firstOrNull()
            assertTrue(effect is TicketContract.Effect.Outer.ShowInfoBottomSheetFragment)

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnStartRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStartRecord)

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 1) { audioRecordController.startRecord() }

            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.Recording)


            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnStopRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStopRecord)

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 1) { audioRecordController.stopRecord() }

            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.None)


            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnStopPendingRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStartRecord)
            dispatch(Message.Outer.OnLockRecord)
            dispatch(Message.Outer.OnStopRecord)

            println(testResult.models)
            assertEquals(4, testResult.models.size)
            coVerify(exactly = 1) { audioRecordController.stopRecord() }

            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.PendingRecord)


            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun onStopPendingRecordTestPendingRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStartRecord)
            dispatch(Message.Outer.OnLockRecord)
            dispatch(Message.Outer.OnStopRecord)

            val mockFile = mockk<File>()
            every { mockFile.path } returns "/mocked/path/file.audio"
            dispatch(Message.Inner.OnAudioRecorded(mockFile))

            println(testResult.models)
            assertEquals(5, testResult.models.size)

            val model = testResult.models.lastOrNull() as? State.Content
            assertEquals(model?.pendingRecord, "/mocked/path/file.audio")


            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun onStopRecordTestPendingRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStartRecord)
            dispatch(Message.Outer.OnStopRecord)

            val tempFile = File.createTempFile("test", ".tmp")
            tempFile.deleteOnExit()

            dispatch(Message.Inner.OnAudioRecorded(tempFile))

            assertEquals(4, testResult.models.size)
            coVerify { repository.addAttachComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES != time)
        }
    }

        @Test
    fun testOnStopEndSendRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStopEndSendRecord)

            assertEquals(2, testResult.models.size)
            coVerify(exactly = 1) { audioRecordController.stopRecord() }

            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.None)


            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnMicShortClicked() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnMicShortClicked)

            assertEquals(2, testResult.models.size)

            val effect = testResult.effects.firstOrNull()
            assertTrue(effect is TicketContract.Effect.Outer.ShowAudioRecordTooltip)

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnCancelRecord() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnCancelRecord)

            assertEquals(2, testResult.models.size)
            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.None)
            coVerify(exactly = 1) { audioRecordController.setRecordCancelledListener(any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnLockRecordIfItIsNotRecording() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnLockRecord)

            assertEquals(2, testResult.models.size)
            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.None)

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnLockRecordIfItIsRecording() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStartRecord)

            dispatch(Message.Outer.OnLockRecord)

            assertEquals(3, testResult.models.size)
            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.HoldRecording)

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testOnRemovePendingAudioClick() = runTest {
        setupMocks()
        val feature = createTicketFeature()

        testComponent(feature, StandardTestDispatcher()) {

            dispatch(Message.Outer.OnStartRecord)

            dispatch(Message.Outer.OnLockRecord)

            assertEquals(3, testResult.models.size)
            val model = testResult.models.lastOrNull() as? State.Content
            assertTrue(model?.recordState is RecordState.HoldRecording)

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    @Test
    fun testSetAttachVariant() = runTest {
        setupMocks()
        val feature = createTicketFeature()
        every { fileManager.copyFile(any()) } returns "url".toUri()

        testComponent(feature, StandardTestDispatcher()) {

            val tempFile = File.createTempFile("test", ".tmp")
            tempFile.deleteOnExit()

            dispatch(Message.Outer.SetAttachVariant("key", tempFile.toUri()))

            assertEquals(2, testResult.models.size)
            coVerify { repository.addAttachComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES != time)
        }
    }

    @Test
    fun testSetAttachVariantWhenUriNull() = runTest {
        setupMocks()
        val feature = createTicketFeature()
        every { fileManager.copyFile(any()) } returns null

        testComponent(feature, StandardTestDispatcher()) {

            val tempFile = File.createTempFile("test", ".tmp")
            tempFile.deleteOnExit()

            dispatch(Message.Outer.SetAttachVariant("key", tempFile.toUri()))

            assertEquals(2, testResult.models.size)
            coVerify (exactly = 0) { repository.addAttachComment(any(), any(), any()) }

            val time = preferencesManager.getLastActiveTime()
            println("lastActiveTime: $time")
            assertTrue(NO_UPDATES == time)
        }
    }

    private fun setupMocks() {

        coEvery {
            repository.getFeed(any(), any(), any())
        } returns Try2.Success(ticket)
        coEvery {
            repository.getFeedFlowByTicketIdFlow(any(), any())
        } returns MutableStateFlow(ticket)
        coEvery { localTicketsStore.getTickets() } returns listOf(ticketEntity)
        coEvery { localTicketsStore.getTicketWithComments(any()) } returns ticketWithComments
        coEvery { draftRepository.getDraft(any()) } returns ""

        coEvery { localTicketsStore.getApplications() } returns listOf(
            ApplicationEntity(
                appId = TEST_APP_ID,
                orgName = "TEST",
                orgLogoUrl = "ff",
                orgDescription = null,
                ratingSettings = null,
                welcomeMessage = "hhh"
            )
        )
    }

    private fun createTicketFeature(): TicketFeature {
        return TicketFeatureFactory(
            accountStore = accountStore,
            storeFactory = storeFactory,
            repository = repository,
            draftRepository = draftRepository,
            router = router,
            fileManager = fileManager,
            preferencesManager = preferencesManager,
            audioRecordControllerFactory = audioRecordControllerFactory,
            audioWrapper = audioWrapper,
            localTicketsStore = localTicketsStore,
            commandsStore = commandsStore,
            systemMessageStore = systemMessageStore,
            idStore = idStore
        ).create(userInternalV1, TEST_TICKET_ID, "weclome", null)
    }
}