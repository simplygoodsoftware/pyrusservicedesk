package utils

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

class TestComponentScope<Model : Any, Intent : Any, Effect : Any>(
    private val testScope: TestScope,
    private val component: Store<Model, Intent, Effect>
) {

    private var testResultInt: TestComponentResultImpl<Model, Effect> = testScope.advanceStep(component)

    val testResult: TestComponentResult<Model, Effect> get() = testResultInt

    fun dispatch(intent: Intent) {
        component.dispatch(intent)
        testScope.advanceStep(component, testResultInt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <Model : Any, Message : Any, Effect : Any> TestScope.advanceStep(
        component: Store<Model, Message, Effect>,
        testResult: TestComponentResultImpl<Model, Effect> = TestComponentResultImpl(),
        testAction: () -> Unit = {},
    ): TestComponentResultImpl<Model, Effect> {

        val modelsJob = launch {
            component.state.toList(testResult.models)
        }

        val effectsJob = launch {
            component.effects.toList(testResult.effects)
        }

        testAction()

        advanceTimeBy(BASE_DELAY_TIME)
        runCurrent()

        modelsJob.cancel()
        effectsJob.cancel()

        return testResult
    }

    companion object {
        private const val BASE_DELAY_TIME = 1000L
    }

}