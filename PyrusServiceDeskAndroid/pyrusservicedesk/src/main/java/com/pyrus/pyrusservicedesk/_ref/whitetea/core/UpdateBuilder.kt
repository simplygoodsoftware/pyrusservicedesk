package com.pyrus.pyrusservicedesk._ref.whitetea.core

internal open class UpdateBuilder<State : Any, Message : Any, Effect : Any>(
    val initialState: State,
) {

    private var currentState: State = initialState
    private val effectsBuilder = OperationsBuilder<Effect>()

    val state: State
        get() = currentState

    /**
     * Set new state
     */
    fun state(update: State.() -> State) {
        currentState = currentState.update()
    }

    /**
     * Add Effect to DCL builder
     * Use unaryPlus inside a function to add a effect
     */
    fun effects(update: OperationsBuilder<Effect>.() -> Unit) {
        effectsBuilder.update()
    }

    internal fun build(): Update<State, Effect> {
        return Update(currentState, effectsBuilder.build())
    }
}