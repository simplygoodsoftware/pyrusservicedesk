package com.pyrus.pyrusservicedesk._ref.whitetea.core

import android.util.Log
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext


internal open class DefaultStore<State : Any, Message : Any, Effect : Any>(
    initialState: State,
    private val initialEffects: List<Effect>,
    private val reducer: L<State, Message, Effect>,
    private val actors: List<Actor<Effect, Message>>,
    private val runtimeContext: CoroutineContext,
    private val actorContext: CoroutineContext,
    private val onCancelCallback: ((state: State) -> Unit)?,
    exceptionHandler: CoroutineExceptionHandler,
): Store<State, Message, Effect> {

    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<State> = mutableState

    private val mutableEffects = MutableSharedFlow<Effect>()
    override val effects: Flow<Effect> = mutableEffects

    private val coroutinesScope = CoroutineScope(runtimeContext + SupervisorJob() + exceptionHandler)

    private val stateUpdateMutex = Mutex()

    override fun init() {
        initialEffects.forEach(::handleEff)
    }

    override fun dispatch(message: Message) {
        Log.d("SDS", "message $message")
        if (!coroutinesScope.isActive) {
            return
        }
        coroutinesScope.launch(runtimeContext) {
            val reducerUpdate = stateUpdateMutex.withLock {
                if (isActive) {
                    val oldState = state.value
                    val update = reducer.update(message, oldState)
                    mutableState.value = update.state
                    update
                } else null
            }
            reducerUpdate?.effects?.forEach { eff ->
                launch {
                    mutableEffects.emit(eff)
                    handleEff(eff)
                }
            }
        }
    }

    override fun cancel() {
        onCancelCallback?.invoke(mutableState.value)
        coroutinesScope.cancel()
    }

    private fun handleEff(effect: Effect) {
        actors.forEach { actor ->
            coroutinesScope.launch(actorContext) {
                actor.handleEffect(effect).collect { dispatch(it) }
            }
        }
    }
}