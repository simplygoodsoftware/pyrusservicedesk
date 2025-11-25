package com.pyrus.pyrusservicedesk._ref.whitetea.core

import android.util.Log
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
        effectAtOnceDelivery: Boolean,
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
                Log.e(name, "store, global error: ${throwable.message}")
                throw throwable
            },
            effectDelivery = effectAtOnceDelivery
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
        effectAtOnceDelivery: Boolean,
        onCancelCallback: ((state: State) -> Unit)?,
    ): Store<State, Message, Effect> {
        return create(
            name = name,
            autoInit = autoInit,
            initialState = initialState,
            initialEffects = initialEffects,
            reducer = reducer,
            actors = listOf(actor),
            effectAtOnceDelivery = effectAtOnceDelivery,
            onCancelCallback = onCancelCallback
        )
    }

}