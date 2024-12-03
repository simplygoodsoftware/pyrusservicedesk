package com.pyrus.pyrusservicedesk._ref.whitetea.core

import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L

internal class DefaultStoreFactory2 : StoreFactory2 {

    override fun <State : Any, Message : Any, Effect : Any> create(
        name: String?,
        autoInit: Boolean,
        initialState: State,
        initialEffects: List<Effect>,
        reducer: L<State, Message, Effect>,
        actors: List<Actor<Effect, Message>>,
        onCancelCallback: ((state: State) -> Unit)?,
    ): Store<State, Message, Effect> {
        return DefaultStore(
            initialState = initialState,
            initialEffects = initialEffects,
            executor = DefaultExecutor(actors),
            updateLogic = reducer,
            onCancelCallback = onCancelCallback
        ).apply {
            if (autoInit) {
                init()
            }
        }
    }

    override fun <State : Any, Message : Any, Effect : Any> create(
        name: String?,
        autoInit: Boolean,
        initialState: State,
        initialEffects: List<Effect>,
        reducer: L<State, Message, Effect>,
        actor: Actor<Effect, Message>,
        onCancelCallback: ((state: State) -> Unit)?,
    ): Store<State, Message, Effect> {
        return create(
            name = name,
            autoInit = autoInit,
            initialState = initialState,
            initialEffects = initialEffects,
            reducer = reducer,
            actors = listOf(actor),
            onCancelCallback = onCancelCallback
        )
    }

}