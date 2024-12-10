package com.pyrus.pyrusservicedesk._ref.whitetea.core

import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L

internal interface StoreFactory {

    fun <State : Any, Message : Any, Effect : Any> create(
        name: String?,
        autoInit: Boolean = true,
        initialState: State,
        initialEffects: List<Effect> = emptyList(),
        executorFactory: () -> Executor<State, Message, Effect>,
        updateLogic: L<State, Message, Effect>,
        onCancelCallback: ((state: State) -> Unit)? = null
    ): Store<State, Message, Effect>

}