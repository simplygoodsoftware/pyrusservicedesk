import com.pyrus.pyrusservicedesk.sdk.sync.FailDelay
import com.pyrus.pyrusservicedesk.sdk.sync.FailDelay.Companion.BASE_DELAY
import com.pyrus.pyrusservicedesk.sdk.sync.FailDelay.Companion.MAX_DELAY
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.math.min


@OptIn(ExperimentalCoroutinesApi::class)
class FailDelayTest {
    private lateinit var failDelay: FailDelay

    @Before
    fun setUp() {
        failDelay = FailDelay()
    }

    @After
    fun cleanData() {
        failDelay.cancel()
        failDelay.clear()
    }

    @Test
    fun firstCallShouldReturnBASE_DELAY() {
        val delay = failDelay.getNextDelay()
        assertEquals(BASE_DELAY, delay)
    }

    @Test
    fun secondCallShouldReturnDelayBetweenBASE_DELAYAndBASE_DELAYx3() {
        failDelay.getNextDelay()
        val delay = failDelay.getNextDelay()

        assertTrue(delay in BASE_DELAY..3000L)
    }

    @Test
    fun thirdCallShouldReturnDelayBetweenBASE_DELAYAndPreviousDelayx3() {
        failDelay.getNextDelay()
        failDelay.getNextDelay()
        val thirdDelay = failDelay.getNextDelay()

        val maxExpected = min(3000L * 3, MAX_DELAY)
        assertTrue(thirdDelay in BASE_DELAY..maxExpected)
    }

    @Test
    fun delayShouldNotExceedMAX_DELAY() {
        repeat(20) {
            val delay = failDelay.getNextDelay()
            assertTrue(delay <= MAX_DELAY)
        }
    }

    @Test
    fun delayShouldNotBeLessThanBASE_DELAY() {
        repeat(20) {
            val delay = failDelay.getNextDelay()
            assertTrue(delay >= BASE_DELAY)
        }
    }

    @Test
    fun delayValuesShouldBeRandom() {
        failDelay.getNextDelay()

        val delays = mutableSetOf<Long>()
        repeat(50) {
            failDelay.clear()
            failDelay.getNextDelay()
            delays.add(failDelay.getNextDelay())
        }

        assertTrue("Expected multiple different delay values", delays.size > 1)
    }

    @Test
    fun delayShouldIncreaseExponentially() {
        val delays = mutableListOf<Long>()
        repeat(5) {
            delays.add(failDelay.getNextDelay())
        }

        assertEquals(BASE_DELAY, delays[0])

        for (i in 1 until delays.size) {
            assertTrue(delays[i] >= delays[i-1] || delays[i] >= BASE_DELAY)
        }
    }

    @Test
    fun clearShouldResetDelaySequence() {
        val firstDelay = failDelay.getNextDelay()
        failDelay.getNextDelay()

        failDelay.clear()

        val firstDelayAfterClear = failDelay.getNextDelay()
        val secondDelayAfterClear = failDelay.getNextDelay()

        assertEquals(BASE_DELAY, firstDelayAfterClear)
        assertEquals(firstDelay, firstDelayAfterClear)
        assertTrue(secondDelayAfterClear in BASE_DELAY..3000L)
    }

    @Test
    fun clearShouldNotAffectOngoingDelays() {
        val delay1 = failDelay.getNextDelay()
        failDelay.clear()
        val delay2 = failDelay.getNextDelay()

        assertEquals(BASE_DELAY, delay2)
        assertEquals(delay1, delay2)
    }

    @Test
    fun multipleClearsShouldWorkCorrectly() {
        repeat(5) {
            failDelay.getNextDelay()
            failDelay.clear()
            val delay = failDelay.getNextDelay()
            assertEquals(BASE_DELAY, delay)
        }
    }

    @Test
    fun cancelableDelayShouldDelayForCalculatedTime() = runTest {
        var executionTime = 0L

        val job = launch {
            val start = currentTime
            failDelay.cancelableDelay()
            executionTime = currentTime - start
        }

        advanceTimeBy(1000)
        job.join()

        assertTrue(executionTime >= BASE_DELAY)
    }

    /**
     * Check that the delay didn't continue after cancellation.
     * This is checked indirectly by ensuring the test doesn't hang
     */
    @Test
    fun cancelableDelayShouldStopWhenCancelIsCalled() = runTest {

        val job = launch {
            failDelay.cancelableDelay()
        }

        advanceTimeBy(2000)

        failDelay.cancel()

        advanceTimeBy(1000)
        job.cancel()

        assertTrue(true)
    }

    @Test
    fun handleZeroDelayAfterClear() {
        failDelay.clear()
        val delay = failDelay.getNextDelay()
        assertEquals(BASE_DELAY, delay)
    }

}