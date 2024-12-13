package com.pyrus.pyrusservicedesk._ref.whitetea.core

import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers

internal class DefaultStoreFactory : StoreFactory {

    override fun <State : Any, Message : Any, Effect : Any> create(
        name: String,
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
            reducer = reducer,
            actors = actors,
            runtimeContext = Dispatchers.Main,
            actorContext = Dispatchers.IO,
            onCancelCallback = onCancelCallback,
            exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
                throw throwable
            },
        ).apply {
            if (autoInit) {
                init()
            }
        }
    }

    override fun <State : Any, Message : Any, Effect : Any> create(
        name: String,
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