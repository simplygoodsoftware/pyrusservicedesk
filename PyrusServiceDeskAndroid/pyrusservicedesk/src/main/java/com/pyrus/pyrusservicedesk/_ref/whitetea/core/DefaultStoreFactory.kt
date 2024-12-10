package com.pyrus.pyrusservicedesk._ref.whitetea.core

import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L

internal class DefaultStoreFactory : StoreFactory {

    override fun <State : Any, Message : Any, Effect : Any> create(
        name: String?,
        autoInit: Boolean,
        initialState: State,
        initialEffects: List<Effect>,
        executorFactory: () -> Executor<State, Message, Effect>,
        updateLogic: L<State, Message, Effect>,
        onCancelCallback: ((state: State) -> Unit)?
    ): Store<State, Message, Effect> {
        return DefaultStore(
            initialState = initialState,
            initialEffects = initialEffects,
            executor = executorFactory(),
            updateLogic = updateLogic,
            onCancelCallback = onCancelCallback
        ).apply {
            if (autoInit) {
                init()
            }
        }
    }
}