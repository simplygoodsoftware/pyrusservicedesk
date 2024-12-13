package com.pyrus.pyrusservicedesk._ref.whitetea.core

import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L

internal interface StoreFactory {

    fun <State : Any, Message : Any, Effect : Any> create(
        name: String,
        autoInit: Boolean = true,
        initialState: State,
        initialEffects: List<Effect> = emptyList(),
        reducer: L<State, Message, Effect>,
        actors: List<Actor<Effect, Message>>,
        onCancelCallback: ((state: State) -> Unit)? = null
    ): Store<State, Message, Effect>

    fun <State : Any, Message : Any, Effect : Any> create(
        name: String,
        autoInit: Boolean = true,
        initialState: State,
        initialEffects: List<Effect> = emptyList(),
        reducer: L<State, Message, Effect>,
        actor: Actor<Effect, Message>,
        onCancelCallback: ((state: State) -> Unit)? = null
    ): Store<State, Message, Effect>

}