package utils

import com.github.terrakok.cicerone.Screen
import com.pyrus.pyrusservicedesk._ref.utils.navigation.ScreenUtils.extractName
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun assertScreen(expectedScreenName: String, screen: Screen) {
    assertEquals(expectedScreenName, extractName(screen.screenKey))
}

fun <Model : Any, Message : Any, Effect : Any> testComponent(
    component: Store<Model, Message, Effect>,
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: Duration = 60.seconds,
    testBody: suspend TestComponentScope<Model, Message, Effect>.(Store<Model, Message, Effect>) -> Unit,
) {
    return runTest(context, timeout) {
        val testScope = TestComponentScope(this, component)
        testBody(testScope, component)
        component.cancel()
    }
}

