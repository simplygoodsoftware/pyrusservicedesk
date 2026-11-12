import android.media.session.PlaybackState
import androidx.media3.session.MediaSession
import com.pyrus.pyrusservicedesk._ref.helpers.DownloadHelper
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File

class AudioWrapperTest {


    private lateinit var audioWrapper: AudioWrapper
    private lateinit var mediaSession: MediaSession

    private lateinit var downloadHelper: DownloadHelper
    private val testDispatcher = StandardTestDispatcher()
    private val testUri = "test_audio_uri"



    @Before
    fun setUp() {
        mediaSession = mockk(relaxed = true)
        downloadHelper = mockk(relaxed = true, relaxUnitFun = true)

        audioWrapper = AudioWrapper(
            session = mediaSession,
            downloadHelper = downloadHelper,
            coroutineScope = TestScope(testDispatcher)
        )
    }

    @Test
    fun shouldCancelDownloadingJobWhenPressStopDownloading() = runTest {
        audioWrapper.downloadingFiles.add(testUri)
        audioWrapper.downloadingFileJob = launch {
            while (true) {
                //imitation of downloading file
            }
        }

        val testDir = File.createTempFile("test", "").parentFile!!
        every { downloadHelper.userAttachmentsDir() } returns testDir

        audioWrapper.playAudio(testUri, testUri)

        assertFalse(audioWrapper.downloadingFiles.contains(testUri))
        assertTrue(audioWrapper.downloadingFileJob?.isCancelled == true)
    }

    @Test
    fun shouldTriggerEventWhenPressStopDownloading() = runTest {
        audioWrapper.downloadingFiles.add(testUri)
        val stateInStart = audioWrapper.getEventState()

        val testDir = File.createTempFile("test", "").parentFile!!
        every { downloadHelper.userAttachmentsDir() } returns testDir

        audioWrapper.playAudio(testUri, testUri)

        val eventState = audioWrapper.getEventState()
        assertEquals(1, eventState)
        assertTrue(stateInStart < eventState)
    }

    @Test
    fun shouldFixDownloadingFilesListWhenPresStopDownloadingSame() = runTest {
        audioWrapper.downloadingFiles.add(testUri)

        val testDir = File.createTempFile("test", "").parentFile!!
        every { downloadHelper.userAttachmentsDir() } returns testDir

        audioWrapper.playAudio(testUri, testUri)

        assertFalse(audioWrapper.downloadingFiles.contains(testUri))
    }

    @Test
    fun shouldSetInitialPlaybackState() {
        val state = audioWrapper.getPlaybackState()

        assertEquals(PlaybackState.STATE_NONE, state)
    }
}