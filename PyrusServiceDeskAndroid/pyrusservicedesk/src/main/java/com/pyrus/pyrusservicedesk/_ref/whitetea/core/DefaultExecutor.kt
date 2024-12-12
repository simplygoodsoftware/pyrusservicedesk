package com.pyrus.pyrusservicedesk._ref.whitetea.core

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Executor.Callbacks
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

internal open class DefaultExecutor<State : Any, Message : Any, Effect : Any>(
    private val actors: List<Actor<Effect, Message>>,
    private val runtimeContext: CoroutineContext = Dispatchers.Main,
    private val renderContext: CoroutineContext = Dispatchers.Main,
    private val actorContext: CoroutineContext = Dispatchers.IO,
    private val exceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        throw throwable
    },
) : CoroutineScope, Executor<State, Message, Effect> {

    override val coroutineContext: CoroutineContext = runtimeContext + SupervisorJob() + exceptionHandler

    private val callbacks = AtomicReference<Callbacks<State, Message, Effect>>()

    override fun init(callbacks: Callbacks<State, Message, Effect>) {
        this.callbacks.set(callbacks)
    }

    override fun handleEffect(effect: Effect) {
        actors.forEach { effHandler ->
            launch(actorContext + exceptionHandler) {
                effHandler
                    .handleEffect(effect = effect)
                    .collect { executeMessage(it) }
            }
        }
    }

    override fun executeMessage(message: Message) {
        if (isActive) {
            // We use current state for update exactly because we don't want to play message on the outdated information
            launch(runtimeContext) { step(callbacks.get().onCreateUpdate(message)) }
        }
    }

    override fun cancel() {
        val job = coroutineContext[Job] ?: error("Scope cannot be cancelled because it does not have a job: $this")
        job.cancel(null)
    }

    private fun step(next: Update<State, Effect>) {
        Log.d("SDS", "state: ${next.state}")

        val renderState = next.state

        val effects = next.effects

        launch(renderContext) {
            callbacks.get().onState(renderState)
            for (effect in effects) {
                callbacks.get().onEffect(effect)
            }
        }

        effects.forEach(::handleEffect)
    }

}