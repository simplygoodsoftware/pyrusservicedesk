package com.pyrus.pyrusservicedesk

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.pyrus.pyrusservicedesk.core.DiInjector
import com.pyrus.pyrusservicedesk.core.UiGraphViewModel
import com.pyrus.pyrusservicedesk.core.UiInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Covers the UI graph lifecycle after the ViewModel-scoped refactor:
 *  - the null-safe accessors ([PyrusServiceDesk.uiInjector] / [PyrusServiceDesk.uiInjectorOrNull]);
 *  - publish / clear with the identity check that protects a freshly launched activity from a
 *    finishing one tearing the graph down (the 1.8.13 `uiInjector() == null` crash / close-reopen
 *    overlap);
 *  - the [UiGraphViewModel] ownership: it publishes on creation, survives configuration changes
 *    (retention), and closes exactly once on real finish (ViewModelStore.clear()).
 */
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

    // region accessor contract

    @Test
    fun uiInjectorOrNullIsNullBeforeUiIsPublished() {
        PyrusServiceDesk.init(application, "appA")
        assertNull(PyrusServiceDesk.uiInjectorOrNull())
    }

    @Test
    fun uiInjectorThrowsBeforeUiIsPublished() {
        PyrusServiceDesk.init(application, "appA")
        assertThrowsIllegalState { PyrusServiceDesk.uiInjector() }
    }

    @Test
    fun publishUiInjectorMakesAccessorsReturnTheSameInstance() {
        PyrusServiceDesk.init(application, "appA")
        val ui = coreCreateUi()

        PyrusServiceDesk.publishUiInjector(ui)

        assertSame(ui, PyrusServiceDesk.uiInjector())
        assertSame(ui, PyrusServiceDesk.uiInjectorOrNull())
        assertSame(ui, PyrusServiceDesk.UI_INJECTOR)

        PyrusServiceDesk.clearUiInjector(ui)
    }

    // endregion

    // region publish / clear + identity check (overlap-race protection)

    @Test
    fun clearUiInjectorDetachesTheStaticForTheCurrentInstance() {
        PyrusServiceDesk.init(application, "appA")
        val ui = coreCreateUi()
        PyrusServiceDesk.publishUiInjector(ui)

        PyrusServiceDesk.clearUiInjector(ui)

        assertNull(PyrusServiceDesk.UI_INJECTOR)
        assertThrowsIllegalState { PyrusServiceDesk.uiInjector() }
    }

    @Test
    fun clearingAStaleGraphMustNotDetachTheFreshlyPublishedOne() {
        // Close/reopen overlap: session A publishes, then session B publishes (static -> B).
        // When the finishing session A clears, the identity check must keep B alive.
        PyrusServiceDesk.init(application, "appA")
        val a = coreCreateUi()
        PyrusServiceDesk.publishUiInjector(a)
        val b = coreCreateUi()
        PyrusServiceDesk.publishUiInjector(b)

        PyrusServiceDesk.clearUiInjector(a) // stale (old) session teardown

        assertSame(
            "Clearing the old graph must not detach the newly published one",
            b,
            PyrusServiceDesk.UI_INJECTOR,
        )

        PyrusServiceDesk.clearUiInjector(b)
        assertNull(PyrusServiceDesk.UI_INJECTOR)
    }

    // endregion

    // region interplay with stop() / init()

    @Test
    fun stopDoesNotDetachUiInjector() {
        PyrusServiceDesk.init(application, "appA")
        val ui = coreCreateUi()
        PyrusServiceDesk.publishUiInjector(ui)

        PyrusServiceDesk.stop()

        assertSame("stop() must not detach the UI graph while UI is alive", ui, PyrusServiceDesk.UI_INJECTOR)
        PyrusServiceDesk.clearUiInjector(ui)
    }

    @Test
    fun initWithChangedCredentialsWhileUiIsPublishedKeepsUiInjector() {
        PyrusServiceDesk.init(application, "appA")
        val ui = coreCreateUi()
        PyrusServiceDesk.publishUiInjector(ui)

        PyrusServiceDesk.init(application, "appB")

        assertSame(
            "init() with changed credentials while UI is alive must only signal stop and keep the graph",
            ui,
            PyrusServiceDesk.UI_INJECTOR,
        )
        PyrusServiceDesk.clearUiInjector(ui)
    }

    // endregion

    // region core.createUiInjector thread affinity (UiInjector requires the main thread)

    @Test
    fun createUiInjectorFromCoreInjectorOnMainThreadSucceeds() {
        PyrusServiceDesk.init(application, "appA")

        val ui = coreCreateUi()

        assertNotNull(ui)
        ui.close()
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
        assertNotNull("createUiInjector() from a background thread must throw", thrown)
        assertTrue("Expected IllegalStateException, got: $thrown", thrown is IllegalStateException)
    }

    // endregion

    // region UiGraphViewModel ownership (the real lifecycle mechanism)

    @Test
    fun uiGraphViewModelPublishesGraphOnCreation() {
        PyrusServiceDesk.init(application, "appA")
        val store = ViewModelStore()

        obtainUiGraphViewModel(store)

        assertNotNull("Creating the UiGraphViewModel must publish the graph", PyrusServiceDesk.UI_INJECTOR)
        assertSame(PyrusServiceDesk.UI_INJECTOR, PyrusServiceDesk.uiInjector())

        store.clear()
    }

    @Test
    fun uiGraphViewModelIsRetainedAcrossReObtainAndGraphIsNotRebuilt() {
        // A configuration change re-obtains the ViewModel from the same store: it must be the same
        // instance and must NOT rebuild the graph (Picasso / ExoPlayer / Cicerone survive rotation).
        PyrusServiceDesk.init(application, "appA")
        val store = ViewModelStore()

        val vm1 = obtainUiGraphViewModel(store)
        val graphAfterFirst = PyrusServiceDesk.UI_INJECTOR
        val vm2 = obtainUiGraphViewModel(store)

        assertSame("Same store must return the retained ViewModel", vm1, vm2)
        assertSame("Rotation must not rebuild the UI graph", graphAfterFirst, PyrusServiceDesk.UI_INJECTOR)

        store.clear()
    }

    @Test
    fun clearingViewModelStoreClosesAndDetachesGraph() {
        PyrusServiceDesk.init(application, "appA")
        val store = ViewModelStore()
        obtainUiGraphViewModel(store)
        assertNotNull(PyrusServiceDesk.UI_INJECTOR)

        store.clear() // real finish -> onCleared

        assertNull("Finishing (store.clear) must detach the graph", PyrusServiceDesk.UI_INJECTOR)
    }

    @Test
    fun overlappingSessionsOldViewModelClearDoesNotTearDownNewGraph() {
        // The exact 1.8.13 crash shape, at the ViewModel level: session A open, session B launched,
        // then A finishes. A's onCleared must not detach B's freshly published graph.
        PyrusServiceDesk.init(application, "appA")
        val storeA = ViewModelStore()
        val storeB = ViewModelStore()

        obtainUiGraphViewModel(storeA)
        val graphA = PyrusServiceDesk.UI_INJECTOR
        obtainUiGraphViewModel(storeB)
        val graphB = PyrusServiceDesk.UI_INJECTOR

        assertNotNull(graphA)
        assertNotNull(graphB)
        assertNotSame("Each session owns its own graph", graphA, graphB)

        storeA.clear() // old session finishes AFTER the new one started

        assertSame("Old session teardown must not detach the new session's graph", graphB, PyrusServiceDesk.UI_INJECTOR)

        storeB.clear()
        assertNull(PyrusServiceDesk.UI_INJECTOR)
    }

    // endregion

    private fun coreCreateUi(): UiInjector =
        (PyrusServiceDesk.INJECTOR as DiInjector).createUiInjector()

    private fun obtainUiGraphViewModel(store: ViewModelStore): UiGraphViewModel =
        ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory(application),
        )[UiGraphViewModel::class.java]

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
