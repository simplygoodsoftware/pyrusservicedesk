package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.DiInjector
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.core.getAppId
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.OnStopCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PSDJvmStaticTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        resetCompanionState()
        resetStaticRepository()
    }

    @After
    fun tearDown() {
        runCatching { (PyrusServiceDesk.INJECTOR as DiInjector?)?.onCancel() }
        resetCompanionState()
        resetStaticRepository()
    }

    // These talk to StaticRepository. They must work before, after, and without
    // any init() at all, and from any thread.

    @Test
    fun registerFileChooserStoresTheChooserBeforeInit() {
        val chooser = mock(FileChooser::class.java)
        PyrusServiceDesk.registerFileChooser(chooser)
        assertSame(chooser, StaticRepository.FILE_CHOOSER)
    }

    @Test
    fun registerFileChooserWithNullClearsTheChooser() {
        StaticRepository.FILE_CHOOSER = mock(FileChooser::class.java)
        PyrusServiceDesk.registerFileChooser(null)
        assertNull(StaticRepository.FILE_CHOOSER)
    }

    @Test
    fun setFieldsDataStoresExtrasBeforeInit() {
        val extras = mapOf("a" to "1", "b" to "2")
        PyrusServiceDesk.setFieldsData(extras)
        assertEquals(extras, StaticRepository.EXTRA_FIELDS)
    }

    @Test
    fun setFieldsDataWithNullClearsExtras() {
        StaticRepository.EXTRA_FIELDS = mapOf("x" to "y")
        PyrusServiceDesk.setFieldsData(null)
        assertNull(StaticRepository.EXTRA_FIELDS)
    }

    @Test
    fun onAuthorizationFailedSetsTheRunnableAcceptsNull() {
        val r = Runnable {
            print("runnable called")
        }
        PyrusServiceDesk.onAuthorizationFailed(r)
        assertSame(r, PyrusServiceDesk.onAuthorizationFailed)

        PyrusServiceDesk.onAuthorizationFailed(null)
        assertNull(PyrusServiceDesk.onAuthorizationFailed)
    }

    @Test
    fun defaultOnAuthorizationFailedStopsServiceDeskViaStop() = runTest {
        // By default onAuthorizationFailed is Runnable { stop() }
        PyrusServiceDesk.init(application, "appA")
        val bus = (PyrusServiceDesk.INJECTOR as DiInjector).finishEventBus

        PyrusServiceDesk.onAuthorizationFailed?.run()

        val event = bus.events().first()
        assertTrue("Default onAuthorizationFailed must call stop() and post finishEventBus(true)", event)
    }

    @Test
    fun sdIsOpenFlowExposesTheUnderlyingStateAndStartsAsFalse() {
        assertSame(PyrusServiceDesk.sdIsOpen, PyrusServiceDesk.sdIsOpenFlow())
        assertFalse(PyrusServiceDesk.sdIsOpenFlow().value)
    }

    @Test
    fun pureSettersAreSafeToCallFromABackgroundThread() = runTest {
        val chooser = mock(FileChooser::class.java)
        val r = Runnable { }
        launch(Dispatchers.IO) {
            PyrusServiceDesk.registerFileChooser(chooser)
            PyrusServiceDesk.setFieldsData(mapOf("k" to "v"))
            PyrusServiceDesk.onAuthorizationFailed(r)
        }.join()

        assertSame(chooser, StaticRepository.FILE_CHOOSER)
        assertEquals(mapOf("k" to "v"), StaticRepository.EXTRA_FIELDS)
        assertSame(r, PyrusServiceDesk.onAuthorizationFailed)
    }

    // What every method does when init() has never been called.
    @Test
    fun startBeforeInitThrowsIllegalStateException() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertThrowsIllegalState {
            PyrusServiceDesk.start(activity)
        }
    }

    @Test
    fun setPushTokenBeforeInitThrowsIllegalStateException() {
        assertThrowsIllegalState {
            PyrusServiceDesk.setPushToken("token", null)
        }
    }

    @Test
    fun stopBeforeInitIsANoAction() {
        // Must not throw and must not flip sdIsOpen from its initial value.
        PyrusServiceDesk.stop()
        assertFalse(PyrusServiceDesk.sdIsOpen.value)
        assertNull(PyrusServiceDesk.INJECTOR)
    }

    @Test
    fun refreshBeforeInitIsANoAction() {
        PyrusServiceDesk.refresh()
        assertNull(PyrusServiceDesk.INJECTOR)
    }

    @Test
    fun subscribeToRepliesBeforeInitThrowsBecauseInjectorIsRequired() {
        // subscribeOnReply asks injector().localTicketsStore inline.
        val sub = mock(NewReplySubscriber::class.java)
        assertThrowsIllegalState {
            PyrusServiceDesk.subscribeToReplies(sub)
        }
    }

    @Test
    fun unsubscribeFromRepliesBeforeInitIsSafe() {
        val sub = mock(NewReplySubscriber::class.java)
        PyrusServiceDesk.unsubscribeFromReplies(sub)
        // No exception, no state mutation visible from outside.
        //In old sd we had java.lang.IllegalStateException: Instantiate PyrusServiceDesk first
    }

    // init basics

    @Test
    fun initV1CreatesAnInjectorWithV1Account() {
        PyrusServiceDesk.init(application, "appA")
        val injector = PyrusServiceDesk.INJECTOR as DiInjector
        val account = injector.accountStore.getAccount()
        assertTrue("Expected V1 account, was $account", account is Account.V1)
        assertEquals("appA", account.getAppId())
        // For V1 account `getUserId()` returns the synthetic instanceId (UUID), never null.
        // It must NOT equal an explicit userId passed in V2 init though.
        val userId = account.getUserId()
        assertNotNull("V1 instanceId-derived userId must not be null", userId)
        assertNotEquals("V1 must not have an explicit user id from caller", "user-1", userId)
    }

    @Test
    fun initV2CreatesAnInjectorWithV2Account() {
        PyrusServiceDesk.init(application, "appB", "user1", "secret-key")
        val injector = PyrusServiceDesk.INJECTOR as DiInjector
        val account = injector.accountStore.getAccount()
        assertTrue("Expected V2 account, was $account", account is Account.V2)
        assertEquals("appB", account.getAppId())
        assertEquals("user1", account.getUserId())
    }

    @Test
    fun secondInitReplacesThePreviousInjector() {
        PyrusServiceDesk.init(application, "appA")
        val first = PyrusServiceDesk.INJECTOR
        PyrusServiceDesk.init(application, "appA-2")
        val second = PyrusServiceDesk.INJECTOR
        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second)
    }

    @Test
    fun initFromABackgroundThreadDoesNotCrash() = runTest {
        val crash = AtomicReference<Throwable?>(null)
        launch(Dispatchers.IO) {
            try {
                PyrusServiceDesk.init(application, "appBg")
            }
            catch (e: Throwable) {
                crash.set(e)
            }
        }.join()
        advanceTimeBy(5000L)
        runCurrent()
        crash.get()?.let { fail("init() from background crashed: $it") }
        assertNotNull(PyrusServiceDesk.INJECTOR)
    }

    @Test
    fun initDoesNotFlipSdIsOpenOnItsOwn() {
        PyrusServiceDesk.init(application, "appA")
        assertFalse(PyrusServiceDesk.sdIsOpen.value)
    }

    // init while UI is open

    @Test
    fun initWithSameCredentialsWhileUIIsOpenIsANoAction() {
        PyrusServiceDesk.init(application, "appA")
        val before = PyrusServiceDesk.INJECTOR
        // Pretend UI is open without actually starting MainActivity.
        PyrusServiceDesk.sdIsOpen.value = true

        PyrusServiceDesk.init(application, "appA")
        val after = PyrusServiceDesk.INJECTOR

        assertSame("Same credentials -> injector must be the same", before, after)
    }

    @Test
    fun initWithDifferentCredentialsWhileUIIsOpenKeepsInjectorAndSignalsStop() {
        PyrusServiceDesk.init(application, "appA")
        val before = PyrusServiceDesk.INJECTOR as DiInjector
        PyrusServiceDesk.sdIsOpen.value = true

        // Capture finishEventBus state before/after.
        val finishBus = before.finishEventBus
        val initialFinish = finishBus.events()
        assertFalse("finishEventBus must start as false", runBlocking { initialFinish.first() })

        PyrusServiceDesk.init(application, "appB")
        val after = PyrusServiceDesk.INJECTOR

        // We don't tear down the injector synchronously (UI still alive).
        assertSame("Existing injector must remain to avoid crashing live UI", before, after)
        // But we did publish a finish request to the bus that AccessDeniedFeature listens to.
        val finishValue = runBlocking { finishBus.events().first() }
        assertTrue("stop() must have posted finishEventBus(true)", finishValue)
    }

    //stop / refresh / start

    @Test
    fun stopAfterInitPostsFinishOnFinishEventBus() = runTest {
        PyrusServiceDesk.init(application, "appA")
        val bus = (PyrusServiceDesk.INJECTOR as DiInjector).finishEventBus

        PyrusServiceDesk.stop()

        val event = bus.events().first()
        assertTrue("finishEventBus must have value=true after stop()", event)
    }

    @Test
    fun refreshIsRateLimitedButNeverCrashes() {
        PyrusServiceDesk.init(application, "appA")
        // 25 rapid refresh calls: more than REFRESH_MAX_COUNT (20) — must not throw.
        repeat(25) { PyrusServiceDesk.refresh() }
    }

    @Test
    fun startAfterInitLaunchesMainActivityFlipsSdIsOpenAndStoresCallback() {
        PyrusServiceDesk.init(application, "appA")
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = OnStopCallback { println("callback called") }

        PyrusServiceDesk.start(activity, onStopCallback = callback)

        // sdIsOpen flipped to true
        assertTrue(PyrusServiceDesk.sdIsOpen.value)
        // onStopCallback stored
        assertSame(callback, PyrusServiceDesk.onStopCallback)
        // MainActivity intent issued
        val nextIntent: Intent? = shadowOf(activity).peekNextStartedActivity()
        assertNotNull("start() must call activity.startActivity(MainActivity)", nextIntent)
        assertEquals("com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.MainActivity", nextIntent!!.component?.className)
        assertTrue(
            "Intent must include FLAG_ACTIVITY_NEW_TASK",
            nextIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == Intent.FLAG_ACTIVITY_NEW_TASK,
        )
    }

    @Test
    fun internalOnServiceDeskStopFiresOnStopCallbackOnceAndClearsIt() {
        PyrusServiceDesk.init(application, "appA")
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val callback = mock(OnStopCallback::class.java)
        PyrusServiceDesk.start(activity, onStopCallback = callback)

        // The lifecycle hook called by MainActivity.finish / RootFragment teardown.
        PyrusServiceDesk.onServiceDeskStop()

        verify(callback, times(1)).onServiceDeskStop()
        assertNull(PyrusServiceDesk.onStopCallback)
        assertFalse(PyrusServiceDesk.sdIsOpen.value)

        // Calling it again must NOT re-fire the callback.
        PyrusServiceDesk.onServiceDeskStop()
        verify(callback, times(1)).onServiceDeskStop()
    }

    // replies subscription

    @Test
    fun subscribeAndUnsubscribeRepliesAfterInitDoNotCrash() {
        PyrusServiceDesk.init(application, "appA")
        val sub = mock(NewReplySubscriber::class.java)

        PyrusServiceDesk.subscribeToReplies(sub)
        PyrusServiceDesk.unsubscribeFromReplies(sub)
        // Re-unsubscribe must be idempotent.
        PyrusServiceDesk.unsubscribeFromReplies(sub)
    }

    @Test
    fun unsubscribeFromRepliesForAnUnknownSubscriberAfterInitIsSafe() {
        PyrusServiceDesk.init(application, "appA")
        val sub = mock(NewReplySubscriber::class.java)
        PyrusServiceDesk.unsubscribeFromReplies(sub)
    }

    //concurrency / ordering

    @Test
    fun concurrentInitsFromManyThreadsDoNotCrashAndEndUpWithOneInjector() = runTest {
        val appNames = (1..8).map { "appCC-$it" }

        val jobs = appNames.map { appName ->
            launch(Dispatchers.IO) {
                PyrusServiceDesk.init(application, appName)
            }
        }

        jobs.joinAll()

        // Last writer wins; we just demand no crash and a non-null injector.
        assertNotNull(PyrusServiceDesk.INJECTOR)
    }

    @Test
    fun setPushTokenFromABackgroundThreadAfterInitDoesNotCrash() = runTest {
        PyrusServiceDesk.init(application, "appA", "user-1", "key-1")
        launch(Dispatchers.IO) {
            PyrusServiceDesk.setPushToken(null, null)
            PyrusServiceDesk.setPushToken("real-token", null)
        }.join()
        // No crash, injector still alive.
        assertNotNull(PyrusServiceDesk.INJECTOR)
    }

    @Test
    fun stopAndRefreshFromBackgroundAfterInitDoNotCrash() = runTest {
        PyrusServiceDesk.init(application, "appA")
        launch(Dispatchers.IO) {
            PyrusServiceDesk.refresh()
            PyrusServiceDesk.stop()
        }.join()
        assertNotNull(PyrusServiceDesk.INJECTOR)
    }

    @Test
    fun initThenStopThenInitAgainCreatesANewInjector() {
        PyrusServiceDesk.init(application, "appA")
        val first = PyrusServiceDesk.INJECTOR
        PyrusServiceDesk.stop()
        // stop() does NOT null out INJECTOR; only signals UI to finish.
        assertSame(first, PyrusServiceDesk.INJECTOR)

        PyrusServiceDesk.init(application, "appA")
        val second = PyrusServiceDesk.INJECTOR
        assertNotEquals(
            "init after stop must rebuild the injector when UI is no longer open",
            first,
            second,
        )
    }

    @Test
    fun pureSettersAreIndependentFromInjectorLifecycle() {
        val chooser = mock(FileChooser::class.java)
        PyrusServiceDesk.registerFileChooser(chooser)
        PyrusServiceDesk.init(application, "appA")
        assertSame(chooser, StaticRepository.FILE_CHOOSER)

        PyrusServiceDesk.init(application, "appB")
        assertSame(chooser, StaticRepository.FILE_CHOOSER)

        PyrusServiceDesk.stop()
        assertSame(chooser, StaticRepository.FILE_CHOOSER)
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

    private fun resetStaticRepository() {
        StaticRepository.logging = false
        StaticRepository.EXTRA_FIELDS = null
        StaticRepository.FILE_CHOOSER = null
        StaticRepository.setConfiguration(null)
    }

}
