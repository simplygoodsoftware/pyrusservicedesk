@file:JvmName("BinderKt")

package com.pyrus.pyrusservicedesk._ref.whitetea.bind

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import com.pyrus.pyrusservicedesk._ref.whitetea.core.EffectHandler
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.attachTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A builder function for the [Binder]
 *
 * @param mainContext a main CoroutineContext, the default value is Dispatchers.Main
 * @param builder the DSL block function
 *
 * 'Весь биндинг происходит здесь' by @AlexSukhobrusov
 *
 * @return a new instance of the [Binder]
 */
internal fun bind(lifecycleScope: CoroutineScope, mainContext: CoroutineContext = Dispatchers.Main, builder: BindingsBuilder.() -> Unit): Binder =
    BuilderBinder(lifecycleScope, mainContext)
        .also(builder)

/**
 * A builder function for the [Binder]. Also attaches the created [Binder] to the provided [Lifecycle].
 * See [Binder.attachTo(...)][attachTo] for more information.
 *
 * @param lifecycle a [Lifecycle] to attach the created [Binder] to
 * @param mode a [BinderLifecycleMode] to be used when attaching the created [Binder] to the [Lifecycle]
 * @param mainContext a main CoroutineContext, the default value is Dispatchers.Main
 * @param builder the DSL block function
 *
 * @return a new instance of the [Binder]
 */
internal fun bind(
    lifecycleScope: LifecycleCoroutineScope,
    lifecycle: Lifecycle,
    mode: BinderLifecycleMode = BinderLifecycleMode.START_STOP,
    mainContext: CoroutineContext = Dispatchers.Main,
    builder: BindingsBuilder.() -> Unit
): Binder =
    bind(lifecycleScope, mainContext, builder).attachTo(lifecycle, mode)

internal interface BindingsBuilder {

    /**
     * Creates a binding between this [Flow] and the provided `consumer`
     *
     * @receiver a stream of values
     * @param consumer a `consumer` of values
     */
    infix fun <T> Flow<T>.bindTo(consumer: suspend (T) -> Unit)

    /**
     * Creates a binding between this [Flow] and the provided [ViewRenderer]
     *
     * @receiver a stream of the `View Models`
     * @param viewRenderer a [ViewRenderer] that will consume the `View Models`
     */
    infix fun <State : Any> Flow<State>.bindTo(viewRenderer: ViewRenderer<State>)

    infix fun <Effect : Any> Flow<Effect>.bindTo(effectHandler: EffectHandler<Effect>)

    /**
     * Creates a binding between this [Flow] and the provided [Store]
     *
     * @receiver a stream of the [Store] `States`
     * @param store a [Store] that will consume the `Intents`
     */
    infix fun <Message : Any> Flow<Message>.bindTo(store: Store<*, Message, *>)
}

private class BuilderBinder(
    private val scope: CoroutineScope,
    private val mainContext: CoroutineContext
) : BindingsBuilder, Binder {
    private val bindings = ArrayList<Binding<*>>()
    private var job: Job? = null

    override fun <T> Flow<T>.bindTo(consumer: suspend (T) -> Unit) {
        bindings += Binding(this, consumer)
    }

    override fun <State : Any> Flow<State>.bindTo(viewRenderer: ViewRenderer<State>) {
        this bindTo {
            viewRenderer.render(it)
        }
    }

    override fun <Effect : Any> Flow<Effect>.bindTo(effectHandler: EffectHandler<Effect>) {
        this bindTo {
            effectHandler.handleEffect(it)
        }
    }

    override fun <T : Any> Flow<T>.bindTo(store: Store<*, T, *>) {
        this bindTo {
            store.dispatch(it)
        }
    }

    override fun start() {
        job = scope.launch(mainContext) {
            bindings.forEach { binding ->
                start(binding)
            }
        }
    }

    private fun <T> CoroutineScope.start(binding: Binding<T>) {
        launch {
            binding.source.collect {
                if (isActive) {
                    binding.consumer(it)
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}

private class Binding<T>(
    val source: Flow<T>,
    val consumer: suspend (T) -> Unit
)