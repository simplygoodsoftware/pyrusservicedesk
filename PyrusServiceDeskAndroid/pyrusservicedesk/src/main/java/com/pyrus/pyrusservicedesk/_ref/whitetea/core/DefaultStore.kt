package com.pyrus.pyrusservicedesk._ref.whitetea.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L


internal open class DefaultStore<State : Any, Message : Any, Effect : Any>(
    initialState: State,
    private val initialEffects: List<Effect>,
    private val updateLogic: L<State, Message, Effect>,
    private val executor: Executor<State, Message, Effect>,
    private val onCancelCallback: ((state: State) -> Unit)?
): Store<State, Message, Effect> {

    private val states = MutableStateFlow(initialState)
    private val effects = MutableSharedFlow<Effect>()

    override fun init() {
        executor.init(object : Executor.Callbacks<State, Message, Effect> {

            override fun onCreateUpdate(message: Message): Update<State, Effect> {
                return updateLogic.update(message, states.value)
            }

            override suspend fun onEffect(effect: Effect) {
                effects.emit(effect)
            }

            override fun onState(state: State) {
                states.value = state
            }

        })
        initialEffects.forEach(executor::handleEffect)
    }

    override fun states(): StateFlow<State> {
        return states
    }

    override fun effects(): Flow<Effect> {
        return effects
    }

    override fun dispatch(message: Message) {
        executor.executeMessage(message)
    }

    override fun cancel() {
        onCancelCallback?.invoke(states.value)
        executor.cancel()
    }
}