package com.pyrus.pyrusservicedesk

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Covers the UI graph lifecycle after the ViewModel-scoped refactor:
 *  - the null-safe accessors ([PyrusServiceDesk.uiInjector] / [PyrusServiceDesk.uiInjectorOrNull]);
 *  - ref-counted acquire / release: the graph is shared across SDK activities, built on the first
 *    acquire and closed only when the last owner is released, so a finishing / DKA-destroyed activity
 *    can not tear down a graph another activity still uses (the 1.8.13 close/reopen overlap AND the
 *    FilePreview + "Don't keep activities" crash);
 *  - the [UiGraphViewModel] ownership: it acquires on creation, survives configuration changes
 *    (retention), and releases exactly once on real finish (ViewModelStore.clear());
 *  - driven through a real activity lifecycle ([UiGraphHostActivity]): a configuration change keeps
 *    the graph, a real finish closes it, DKA rebuilds it on restore, and a second activity keeps the
 *    graph alive while the owner is destroyed under it.
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
    fun uiInjectorOrNullIsNullBeforeGraphIsAcquired() {
        PyrusServiceDesk.init(application, "appA")
        assertNull(PyrusServiceDesk.uiInjectorOrNull())
    }

    @Test
    fun uiInjectorThrowsBeforeGraphIsAcquired() {
        PyrusServiceDesk.init(application, "appA")
        assertThrowsIllegalState { PyrusServiceDesk.uiInjector() }
    }

    @Test
    fun acquireUiInjectorBuildsAndPublishesTheGraph() {
        PyrusServiceDesk.init(application, "appA")

        val ui = PyrusServiceDesk.acquireUiInjector()

        assertSame(ui, PyrusServiceDesk.uiInjector())
        assertSame(ui, PyrusServiceDesk.uiInjectorOrNull())
        assertSame(ui, PyrusServiceDesk.UI_INJECTOR)

        PyrusServiceDesk.releaseUiInjector()
    }

    // endregion

    // region ref-counted acquire / release (shared graph across SDK activities)

    @Test
    fun releaseUiInjectorClosesAndDetachesTheGraphAtZeroRefs() {
        PyrusServiceDesk.init(application, "appA")
        PyrusServiceDesk.acquireUiInjector()

        PyrusServiceDesk.releaseUiInjector()

        assertNull(PyrusServiceDesk.UI_INJECTOR)
        assertThrowsIllegalState { PyrusServiceDesk.uiInjector() }
    }

    @Test
    fun secondAcquireReusesTheSameGraphAndReleaseKeepsItWhileRefsRemain() {
        // Two SDK activities (MainActivity + FilePreviewActivity) share ONE graph. Acquiring twice
        // must return the SAME instance, and releasing one owner must NOT close it while the other
        // still holds a ref — this is what fixes the FilePreview + "Don't keep activities" crash and
        // subsumes the old close/reopen identity check.
        PyrusServiceDesk.init(application, "appA")

        val first = PyrusServiceDesk.acquireUiInjector()
        val second = PyrusServiceDesk.acquireUiInjector()

        assertSame("Both owners must share the same graph instance", first, second)
        assertSame(first, PyrusServiceDesk.UI_INJECTOR)

        PyrusServiceDesk.releaseUiInjector() // one owner goes away

        assertSame(
            "The graph must survive while another owner still holds a ref",
            first,
            PyrusServiceDesk.UI_INJECTOR,
        )

        PyrusServiceDesk.releaseUiInjector() // last owner goes away
        assertNull(PyrusServiceDesk.UI_INJECTOR)
    }

    @Test
    fun releaseWithoutAcquireIsSafeAndDoesNotUnderflow() {
        PyrusServiceDesk.init(application, "appA")

        // No matching acquire: must clamp at 0, not go negative or crash.
        PyrusServiceDesk.releaseUiInjector()

        assertNull(PyrusServiceDesk.UI_INJECTOR)
        assertEquals(0, PyrusServiceDesk.uiGraphOwnerCountForTest)
    }

    // endregion

    // region interplay with stop() / init()

    @Test
    fun stopDoesNotDetachUiInjector() {
        PyrusServiceDesk.init(application, "appA")
        val ui = PyrusServiceDesk.acquireUiInjector()

        PyrusServiceDesk.stop()

        assertSame("stop() must not detach the UI graph while UI is alive", ui, PyrusServiceDesk.UI_INJECTOR)
        PyrusServiceDesk.releaseUiInjector()
    }

    @Test
    fun initWithChangedCredentialsWhileUiIsPublishedKeepsUiInjector() {
        PyrusServiceDesk.init(application, "appA")
        val ui = PyrusServiceDesk.acquireUiInjector()

        PyrusServiceDesk.init(application, "appB")

        assertSame(
            "init() with changed credentials while UI is alive must only signal stop and keep the graph",
            ui,
            PyrusServiceDesk.UI_INJECTOR,
        )
        PyrusServiceDesk.releaseUiInjector()
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
    fun twoOwnersShareOneGraphAndOldOwnerClearKeepsItAlive() {
        // Two owners at the ViewModel level (a close/reopen overlap, or MainActivity +
        // FilePreviewActivity) share ONE graph via the ref-count. Clearing owner A must NOT detach
        // the graph while owner B still holds it — covering both the 1.8.13 close/reopen overlap and
        // the FilePreview + "Don't keep activities" regression.
        PyrusServiceDesk.init(application, "appA")
        val storeA = ViewModelStore()
        val storeB = ViewModelStore()

        obtainUiGraphViewModel(storeA)
        val graph = PyrusServiceDesk.UI_INJECTOR
        assertNotNull(graph)
        obtainUiGraphViewModel(storeB)

        assertSame("Both owners must share the same graph", graph, PyrusServiceDesk.UI_INJECTOR)

        storeA.clear() // one owner finishes while the other is still alive

        assertSame("A surviving owner must keep the shared graph alive", graph, PyrusServiceDesk.UI_INJECTOR)

        storeB.clear()
        assertNull("The last owner's clear must close and detach the graph", PyrusServiceDesk.UI_INJECTOR)
    }

    // endregion

    // region real activity lifecycle: onCleared honors isFinishing without a manual guard

    @Test
    fun configurationChangeRetainsGraphAndDoesNotFireOnCleared() {
        // The `isFinishing == false` branch of the old onDestroy, now for free: on a configuration
        // change ComponentActivity keeps the ViewModelStore (isChangingConfigurations() == true),
        // so onCleared() must NOT run — the graph survives as the very same instance and is not
        // rebuilt (Picasso / ExoPlayer / Cicerone stay alive across rotation).
        PyrusServiceDesk.init(application, "appA")
        val controller = Robolectric.buildActivity(UiGraphHostActivity::class.java).setup()
        val graph = PyrusServiceDesk.UI_INJECTOR
        assertNotNull("Host activity must publish the graph on create", graph)

        val landscape = Configuration(application.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }
        controller.configurationChange(landscape)

        assertSame(
            "Configuration change must not detach or rebuild the graph (onCleared must not fire)",
            graph,
            PyrusServiceDesk.UI_INJECTOR,
        )

        // A real finish afterwards still tears it down.
        controller.get().finish()
        controller.destroy()
        assertNull(PyrusServiceDesk.UI_INJECTOR)
    }

    @Test
    fun realActivityFinishClosesAndDetachesGraph() {
        // The `isFinishing == true` branch: on a real finish isChangingConfigurations() is false,
        // so the store is cleared, onCleared() runs and the graph is closed + detached — no manual
        // `if (isFinishing) releaseUiInjector()` in onDestroy is needed.
        PyrusServiceDesk.init(application, "appA")
        val controller = Robolectric.buildActivity(UiGraphHostActivity::class.java).setup()
        assertNotNull(PyrusServiceDesk.UI_INJECTOR)

        controller.get().finish()
        controller.destroy()

        assertNull(
            "A real finish must close and detach the graph via UiGraphViewModel.onCleared()",
            PyrusServiceDesk.UI_INJECTOR,
        )
    }

    @Test
    fun dontKeepActivitiesDestroyWithProcessAliveRebuildsAFreshGraphOnRestore() {
        // The one case where onCleared() fires without isFinishing: "Don't keep activities" (or a
        // background per-activity reclaim) while the process stays alive — isFinishing == false AND
        // isChangingConfigurations() == false. The store clear is gated ONLY on
        // isChangingConfigurations(), so the graph is still closed. On return the activity is
        // recreated with a non-null savedInstanceState; because the core INJECTOR outlives the
        // activity, restore does NOT bail — it publishes a brand new graph. The trade-off is a fresh
        // graph (new Picasso mem-cache / player), not a crash and not a dead SD.
        PyrusServiceDesk.init(application, "appA")
        val controller = Robolectric.buildActivity(UiGraphHostActivity::class.java).setup()
        val firstGraph = PyrusServiceDesk.UI_INJECTOR
        assertNotNull(firstGraph)

        // Home under DKA: save state, then destroy WITHOUT finishing and WITHOUT a config change.
        val savedState = Bundle()
        controller.pause().stop().saveInstanceState(savedState).destroy()

        assertNull(
            "DKA destroy (no config change) must clear the store and close the graph",
            PyrusServiceDesk.UI_INJECTOR,
        )
        assertNotNull(
            "Process is alive: the core injector must outlive the activity",
            PyrusServiceDesk.INJECTOR,
        )

        // Return via recents: recreate with the saved (non-null) Bundle.
        val recreated = Robolectric.buildActivity(UiGraphHostActivity::class.java)
            .create(savedState)
            .start()
            .resume()
        val secondGraph = PyrusServiceDesk.UI_INJECTOR

        assertNotNull("Restore must publish a fresh graph, not bail out", secondGraph)
        assertNotSame("The rebuilt graph must be a brand new instance", firstGraph, secondGraph)

        recreated.get().finish()
        recreated.destroy()
        assertNull(PyrusServiceDesk.UI_INJECTOR)
    }

    @Test
    fun secondActivityKeepsGraphAliveWhenOwnerIsDestroyedUnderDka() {
        // The exact FilePreview + "Don't keep activities" shape with real activities: an owner
        // (MainActivity) opens the graph, a consumer (FilePreviewActivity) is launched on top and
        // shares it, then DKA destroys the backgrounded owner. Before the fix the owner's onCleared
        // tore the graph down under the consumer, so returning to the consumer hit UI_INJECTOR==null
        // and crashed in ActivityBase.onCreate. With ref-counting the graph survives while the
        // consumer is alive, and only the last activity's teardown closes it.
        PyrusServiceDesk.init(application, "appA")
        val owner = Robolectric.buildActivity(UiGraphHostActivity::class.java).setup()
        val graph = PyrusServiceDesk.UI_INJECTOR
        assertNotNull(graph)

        val consumer = Robolectric.buildActivity(UiGraphHostActivity::class.java).setup()
        assertSame("Both activities must share the SAME graph", graph, PyrusServiceDesk.UI_INJECTOR)

        // DKA destroys the backgrounded owner (not finishing, not a configuration change).
        owner.pause().stop().destroy()

        assertSame(
            "Owner teardown must NOT close the graph while the consumer is still alive",
            graph,
            PyrusServiceDesk.UI_INJECTOR,
        )

        consumer.get().finish()
        consumer.destroy()
        assertNull("Closing the last activity must close and detach the graph", PyrusServiceDesk.UI_INJECTOR)
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
        runCatching { PyrusServiceDesk.resetUiGraphForTest() }
        runCatching { PyrusServiceDesk.onStopCallback = null }
        runCatching { PyrusServiceDesk.lastRefreshes = ArrayList() }
        PyrusServiceDesk.sdIsOpen.value = false
        PyrusServiceDesk.onAuthorizationFailed { PyrusServiceDesk.stop() }
    }
}

/**
 * Minimal stand-in for the SDK activity: like MainActivity it obtains the activity-scoped
 * [UiGraphViewModel] (whose constructor creates and publishes the UI graph) as the very first step
 * of onCreate, so a real Robolectric lifecycle exercises the same ViewModelStore retain/clear
 * semantics — configuration change, real finish, and "Don't keep activities" (destroy without a
 * configuration change).
 */
internal class UiGraphHostActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ViewModelProvider(this)[UiGraphViewModel::class.java]
        super.onCreate(savedInstanceState)
    }
}
