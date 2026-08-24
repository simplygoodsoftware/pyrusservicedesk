import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHook
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.UploadCancelledException
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class RemoteFileStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var api: FakeUploadApi
    private lateinit var fileStore: RemoteFileStore
    private lateinit var uploadScope: CoroutineScope

    @Before
    fun setUp() {
        api = FakeUploadApi()
        fileStore = RemoteFileStore(api)
        uploadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @After
    fun tearDown() {
        uploadScope.cancel()
    }

    @Test
    fun shouldUploadFile() = runUploadTest {
        val result = upload("first.txt").awaitResult()

        assertTrue(result.isSuccess())
        assertEquals(GUID, (result as Try.Success).value.guid)
        assertEquals(1, api.attempts)
    }

    @Test
    fun shouldNotRetryCancelledUpload() = runUploadTest {
        val hook = uploadFileHook()
        api.gateAttempt(1)

        val upload = upload("first.txt", hook)
        api.awaitAttemptStarted()

        cancelUploading(hook)
        api.openGate()

        assertCancelled(upload.awaitResult())
        assertEquals("a cancelled upload must not be retried", 1, api.attempts)
    }

    @Test
    fun shouldUploadNextQueuedFileAfterCancelOfCurrentOne() = runUploadTest {
        val firstHook = uploadFileHook()
        api.gateAttempt(1)

        val firstUpload = upload("first.txt", firstHook)
        api.awaitAttemptStarted()
        val secondUpload = upload("second.txt")

        cancelUploading(firstHook)
        api.openGate()

        assertCancelled(firstUpload.awaitResult())
        assertTrue(
            "the queued file must be uploaded after the cancelled one",
            secondUpload.awaitResult().isSuccess(),
        )
        assertEquals(listOf("second.txt"), api.uploadedFileNames.toList())
    }

    @Test
    fun shouldResumeCancelledQueuedFileWithoutUploading() = runUploadTest {
        val firstHook = uploadFileHook()
        val secondHook = uploadFileHook()
        api.gateAttempt(1)

        val firstUpload = upload("first.txt", firstHook)
        api.awaitAttemptStarted()
        val secondUpload = upload("second.txt", secondHook)

        cancelUploading(secondHook)

        assertCancelled(secondUpload.awaitResult())
        assertEquals("the file cancelled in the queue must not be uploaded", 1, api.attempts)

        api.openGate()
        assertTrue(firstUpload.awaitResult().isSuccess())
    }

    @Test
    fun shouldUploadNewFileAfterCancelledOne() = runUploadTest {
        val hook = uploadFileHook()
        api.gateAttempt(1)

        val cancelledUpload = upload("first.txt", hook)
        api.awaitAttemptStarted()
        cancelUploading(hook)
        api.openGate()
        assertCancelled(cancelledUpload.awaitResult())

        // isUploading must be released, otherwise the new file waits in the queue forever.
        val result = upload("second.txt").awaitResult()

        assertTrue("the file added after the cancelled one must be uploaded", result.isSuccess())
    }

    @Test
    fun shouldRetryFailedUploadUntilSuccess() = runUploadTest {
        api.resultProvider = { attempt ->
            if (attempt == 1) Try.Failure(UnknownHostException("pyrus.com"))
            else Try.Success(FileUploadResponseData(GUID, null))
        }

        val result = upload("first.txt").awaitResult()

        assertTrue(result.isSuccess())
        assertEquals(2, api.attempts)
    }

    @Test
    fun shouldNotUploadFileWithAlreadyCancelledHook() = runUploadTest {
        val hook = uploadFileHook()
        hook.cancelUploading()

        val result = upload("first.txt", hook).awaitResult()

        assertCancelled(result)
        assertEquals(0, api.attempts)
    }

    @Test
    fun shouldPublishEachProgressPercentOnce() = runUploadTest {
        val progress = CopyOnWriteArrayList<Int>()

        val result = upload("first.txt", bytes = 100 * 1024, progressListener = progress::add)
            .awaitResult()

        assertTrue(result.isSuccess())
        assertEquals("each percent must be published once", progress.distinct(), progress.toList())
        assertEquals(100, progress.last())
        assertTrue("unexpected progress values count: ${progress.size}", progress.size <= 101)
    }

    private fun runUploadTest(body: suspend CoroutineScope.() -> Unit) = runBlocking {
        withTimeout(TEST_TIMEOUT) { body() }
    }

    private fun upload(
        name: String,
        hook: UploadFileHook = uploadFileHook(),
        bytes: Int = FILE_SIZE,
        progressListener: (Int) -> Unit = {},
    ): Deferred<Try<FileUploadResponseData>> {
        val file = createFile(name, bytes)
        return uploadScope.async { fileStore.uploadFile(file, hook, progressListener) }
    }

    /**
     * [UploadFileHook] creates a main thread Handler in its constructor, so the test uses a mock
     * with the same behaviour of the cancellation flag instead of the real hook.
     */
    private fun uploadFileHook(): UploadFileHook {
        var isCancelled = false
        val hook = mockk<UploadFileHook>(relaxed = true)
        every { hook.isCancelled } answers { isCancelled }
        every { hook.cancelUploading() } answers { isCancelled = true }
        return hook
    }

    /** Cancels uploading in the same way as SdRepository.cancelUploadFile does. */
    private fun cancelUploading(hook: UploadFileHook) {
        hook.cancelUploading()
        fileStore.cancelUploading(hook)
    }

    private suspend fun Deferred<Try<FileUploadResponseData>>.awaitResult(): Try<FileUploadResponseData> {
        val result = withTimeoutOrNull(UPLOAD_TIMEOUT) { await() }
        assertNotNull("uploading has not been finished in $UPLOAD_TIMEOUT ms, it is stuck", result)
        return result!!
    }

    private fun createFile(name: String, bytes: Int): File {
        val file = tempFolder.newFile(name)
        file.writeBytes(ByteArray(bytes) { it.toByte() })
        return file
    }

    private fun assertCancelled(result: Try<FileUploadResponseData>) {
        assertTrue("expected a failure, but was $result", result is Try.Failure)
        assertTrue(
            "expected UploadCancelledException, but was ${(result as Try.Failure).error}",
            result.error is UploadCancelledException,
        )
    }

    /**
     * Api that reads the request body like OkHttp does, so the cancellation of [UploadFileHook] is
     * detected by ProgressRequestBody and is delivered to the store as a failure of the call.
     */
    private class FakeUploadApi : ServiceDeskApi {

        var resultProvider: (attempt: Int) -> Try<FileUploadResponseData> = {
            Try.Success(FileUploadResponseData(GUID, null))
        }

        private val attemptCounter = AtomicInteger()
        private val startedAttempts = Channel<Int>(Channel.UNLIMITED)
        private val gate = Channel<Unit>(Channel.UNLIMITED)
        private var gatedAttempt: Int? = null

        val attempts: Int get() = attemptCounter.get()
        val uploadedFileNames = CopyOnWriteArrayList<String>()

        /** Makes the given attempt wait inside the api call until [openGate] is called. */
        fun gateAttempt(attempt: Int) {
            gatedAttempt = attempt
        }

        suspend fun awaitAttemptStarted(): Int = startedAttempts.receive()

        fun openGate() {
            gate.trySend(Unit)
        }

        override suspend fun getTickets(requestBody: RequestBodyBase): Try<TicketsDto> {
            throw UnsupportedOperationException("getTickets is not used in the test")
        }

        override suspend fun uploadFile(file: MultipartBody.Part): Try<FileUploadResponseData> {
            val attempt = attemptCounter.incrementAndGet()
            startedAttempts.trySend(attempt)
            if (attempt == gatedAttempt) gate.receive()

            return try {
                // OkHttp reads the body here, ProgressRequestBody checks the cancellation on read.
                file.body().writeTo(Buffer())
                uploadedFileNames.add(file.fileName())
                resultProvider(attempt)
            }
            catch (e: IOException) {
                // ResultCall maps a failure of the call to Try.Failure in the same way.
                Try.Failure(e)
            }
        }

        private fun MultipartBody.Part.fileName(): String = headers()
            ?.get("Content-Disposition")
            ?.substringAfter("filename=\"", "")
            ?.substringBefore('"')
            .orEmpty()
    }

    private companion object {
        const val GUID = "test-guid"
        const val FILE_SIZE = 4 * 1024
        const val UPLOAD_TIMEOUT = 10_000L
        const val TEST_TIMEOUT = 60_000L
    }

}
