package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.MainActivity
import com.pyrus.pyrusservicedesk.core.DiInjector
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Covers launching the SDK UI from any thread without blocking the caller (the ANR / thread-launch
 * regression). start() must not do a blocking main-thread hop: the actual activity launch is
 * posted to the main thread ([Activity.runOnUiThread]), so a background caller returns immediately.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PSDStartThreadingTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        resetCompanionState()
    }

    @After
    fun tearDown() {
        runCatching { (PyrusServiceDesk.INJECTOR as DiInjector?)?.onCancel() }
        resetCompanionState()
    }

    @Test
    fun startFromBackgroundThreadReturnsWithoutBlockingAndLaunchesOnMain() {
        PyrusServiceDesk.init(application, "appA")
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        val returned = CountDownLatch(1)
        val error = AtomicReference<Throwable?>(null)
        Thread {
            try {
                PyrusServiceDesk.start(activity)
            } catch (t: Throwable) {
                error.set(t)
            } finally {
                returned.countDown()
            }
        }.start()

        // With the previous runBlocking { withContext(Main) } launch, calling start() on a
        // background thread while Robolectric's main looper is paused would deadlock and never
        // return. runOnUiThread posts instead, so start() returns immediately.
        assertTrue(
            "start() from a background thread must not block the caller",
            returned.await(5, TimeUnit.SECONDS),
        )
        error.get()?.let { fail("start() from a background thread threw: $it") }

        // The synchronous part ran on the caller thread...
        assertTrue("start() must flip sdIsOpen synchronously", PyrusServiceDesk.sdIsOpen.value)
        // ...but the activity launch was POSTED to the main thread, not executed on the caller.
        assertNull(
            "activity launch must be posted to the main thread, not run on the background caller",
            shadowOf(activity).peekNextStartedActivity(),
        )

        // Draining the main looper runs the posted launch.
        shadowOf(Looper.getMainLooper()).idle()

        val intent: Intent? = shadowOf(activity).peekNextStartedActivity()
        assertNotNull("the posted launch must run MainActivity on the main thread", intent)
        assertEquals(MainActivity::class.java.name, intent!!.component?.className)
        assertTrue(
            "launch intent must carry FLAG_ACTIVITY_NEW_TASK",
            intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == Intent.FLAG_ACTIVITY_NEW_TASK,
        )
    }

    @Test
    fun startOnMainThreadLaunchesInlineWithoutAnExtraLoop() {
        PyrusServiceDesk.init(application, "appA")
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        PyrusServiceDesk.start(activity)

        // On the main thread runOnUiThread runs inline, so the launch is issued without draining
        // the looper.
        val intent: Intent? = shadowOf(activity).peekNextStartedActivity()
        assertNotNull("start() on the main thread must launch MainActivity inline", intent)
        assertEquals(MainActivity::class.java.name, intent!!.component?.className)
    }

    private fun resetCompanionState() {
        runCatching { PyrusServiceDesk.INSTANCE = null }
        runCatching { PyrusServiceDesk.INJECTOR = null }
        runCatching { while (PyrusServiceDesk.UI_INJECTOR != null) PyrusServiceDesk.releaseUiInjector() }
        runCatching { PyrusServiceDesk.onStopCallback = null }
        runCatching { PyrusServiceDesk.lastRefreshes = ArrayList() }
        PyrusServiceDesk.sdIsOpen.value = false
        PyrusServiceDesk.onAuthorizationFailed { PyrusServiceDesk.stop() }
    }
}
