package com.pyrus.pyrusservicedesk._ref.whitetea.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store

internal class StoreProxy<State1 : Any, Message1 : Any, Effect1 : Any, State2 : Any, Message2 : Any, Effect2 : Any>(
    private val store: Store<State1, Message1, Effect1>,
    stateMapper: (State1) -> State2,
    private val messageMapper: (Message2) -> Message1,
    effectMapper: (Effect1) -> Effect2?,
): Store<State2, Message2, Effect2> {

    override val state: StateFlow<State2> = store.state.mapState(stateMapper)
    override val effects: Flow<Effect2> = store.effects.mapNotNull(effectMapper)

    override fun init() = store.init()

    override fun dispatch(message: Message2) = store.dispatch(messageMapper(message))

    override fun cancel() = store.cancel()

}