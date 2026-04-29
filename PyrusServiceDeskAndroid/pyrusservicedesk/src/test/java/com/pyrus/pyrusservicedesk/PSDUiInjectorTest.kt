package com.pyrus.pyrusservicedesk

import android.app.Application
import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.MainActivity
import com.pyrus.pyrusservicedesk.core.DiInjector
import com.pyrus.pyrusservicedesk.sdk.updates.OnStopCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PSDUiInjectorTest {

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
    fun uiInjectorAccessorThrowsBeforeUiIsStarted() {
        PyrusServiceDesk.init(application, "appA")
        assertThrowsIllegalState {
            PyrusServiceDesk.uiInjector()
        }
    }

    @Test
    fun ensureUiInjectorCreatesAndCachesSingleInstance() {
        PyrusServiceDesk.init(application, "appA")

        val first = PyrusServiceDesk.ensureUiInjector()
        val second = PyrusServiceDesk.ensureUiInjector()

        assertSame(first, second)
        assertSame(first, PyrusServiceDesk.uiInjector())
        assertSame(first, PyrusServiceDesk.UI_INJECTOR)
    }

    @Test
    fun createUiInjectorFromCoreInjectorOnMainThreadSucceeds() {
        PyrusServiceDesk.init(application, "appA")
        val core = PyrusServiceDesk.INJECTOR as DiInjector

        val ui = core.createUiInjector()

        assertNotNull(ui)
        // Avoid leaks in this test: directly close manually created graph.
        ui.close()
    }

    @Test
    fun releaseUiInjectorClearsAccessorAndIsIdempotent() {
        PyrusServiceDesk.init(application, "appA")
        val created = PyrusServiceDesk.ensureUiInjector()
        assertNotNull(created)

        PyrusServiceDesk.releaseUiInjector()
        assertNull(PyrusServiceDesk.UI_INJECTOR)
        assertThrowsIllegalState { PyrusServiceDesk.uiInjector() }

        // second release must be no action
        PyrusServiceDesk.releaseUiInjector()
        assertNull(PyrusServiceDesk.UI_INJECTOR)
    }

    @Test
    fun stopDoesNotReleaseUiInjector() {
        PyrusServiceDesk.init(application, "appA")
        val ui = PyrusServiceDesk.ensureUiInjector()

        PyrusServiceDesk.stop()

        assertSame("stop() must not null UI injector while UI lifecycle is active", ui, PyrusServiceDesk.UI_INJECTOR)
        assertSame(ui, PyrusServiceDesk.uiInjector())
    }

    @Test
    fun initWithChangedCredentialsWhileUiInjectorExistsDoesNotReplaceUiInjector() {
        PyrusServiceDesk.init(application, "appA")
        val uiBefore = PyrusServiceDesk.ensureUiInjector()

        PyrusServiceDesk.init(application, "appB")

        assertSame(
            "init() with changed credentials while UI is alive should only signal stop and keep ui graph intact",
            uiBefore,
            PyrusServiceDesk.UI_INJECTOR
        )
    }

    @Test
    fun ensureUiInjectorOnBackgroundThreadThrows() = runTest {
        PyrusServiceDesk.init(application, "appA")
        val crash = AtomicReference<Throwable?>(null)
        val latch = CountDownLatch(1)
        launch(Dispatchers.IO) {
            try {
                PyrusServiceDesk.ensureUiInjector()
            } catch (t: Throwable) {
                crash.set(t)
            } finally {
                latch.countDown()
            }
        }.join()
        assertTrue("background ensureUiInjector thread timeout", latch.await(5, TimeUnit.SECONDS))
        val thrown = crash.get()
        assertNotNull("ensureUiInjector() from background thread must throw", thrown)
        assertTrue("Expected IllegalStateException, got: $thrown", thrown is IllegalStateException)
    }

    @Test
    fun createUiInjectorFromCoreInjectorOnBackgroundThreadThrows() = runTest {
        PyrusServiceDesk.init(application, "appA")
        val core = PyrusServiceDesk.INJECTOR as DiInjector
        val crash = AtomicReference<Throwable?>(null)
        val latch = CountDownLatch(1)

        launch(Dispatchers.IO) {
            try {
                core.createUiInjector()
            } catch (t: Throwable) {
                crash.set(t)
            } finally {
                latch.countDown()
            }
        }.join()

        assertTrue("background createUiInjector thread timeout", latch.await(5, TimeUnit.SECONDS))
        val thrown = crash.get()
        assertNotNull("createUiInjector() from background thread must throw", thrown)
        assertTrue("Expected IllegalStateException, got: $thrown", thrown is IllegalStateException)
    }

    @Test
    fun startLaunchesMainActivityAndUiInjectorIsCreatedWhenMainActivityCreatesUiGraph() {
        PyrusServiceDesk.init(application, "appA")
        val hostActivity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mock(OnStopCallback::class.java)

        PyrusServiceDesk.start(hostActivity, onStopCallback = callback)

        val launchIntent: Intent? = shadowOf(hostActivity).peekNextStartedActivity()
        assertNotNull("start() must schedule MainActivity launch", launchIntent)
        assertEquals(MainActivity::class.java.name, launchIntent!!.component?.className)

        // In production MainActivity.onCreate() calls ensureUiInjector().
        assertNull("Before MainActivity.onCreate(), UI injector is not created yet", PyrusServiceDesk.UI_INJECTOR)
        PyrusServiceDesk.ensureUiInjector()
        assertNotNull(PyrusServiceDesk.uiInjector())
    }

    private fun assertThrowsIllegalState(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalStateException, none was thrown")
        } catch (expected: IllegalStateException) {
            // ok
        } catch (other: Throwable) {
            fail("Expected IllegalStateException, got: $other")
        }
    }

    private fun resetCompanionState() {
        runCatching { PyrusServiceDesk.INSTANCE = null }
        runCatching { PyrusServiceDesk.INJECTOR = null }
        runCatching { PyrusServiceDesk.UI_INJECTOR = null }
        runCatching { PyrusServiceDesk.onStopCallback = null }
        runCatching { PyrusServiceDesk.lastRefreshes = ArrayList() }
        PyrusServiceDesk.sdIsOpen.value = false
        PyrusServiceDesk.onAuthorizationFailed { PyrusServiceDesk.stop() }
    }
}

