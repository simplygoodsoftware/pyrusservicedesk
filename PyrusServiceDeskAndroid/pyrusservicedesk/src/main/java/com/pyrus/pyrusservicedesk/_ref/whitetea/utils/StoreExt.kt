package com.pyrus.pyrusservicedesk._ref.whitetea.utils

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store


internal fun <State1 : Any, Message1 : Any, Effect1 : Any, State2 : Any, Message2 : Any, Effect2 : Any> Store<State1, Message1, Effect1>.adapt(
    stateMapper: (State1) -> State2,
    messageMapper: (Message2) -> Message1,
    effectMapper: (Effect1) -> Effect2?,
): Store<State2, Message2, Effect2> {
    return StoreProxy(this, stateMapper, messageMapper, effectMapper)
}

internal fun <State : Any, Message : Any, Effect1 : Any, Effect2 : Any> Store<State, Message, Effect1>.adapt(
    effectMapper: (Effect1) -> Effect2?,
): Store<State, Message, Effect2> = adapt({ it }, { it }, effectMapper)