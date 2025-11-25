package com.pyrus.pyrusservicedesk._ref.whitetea.core.transition

internal class TransitionSpace<State : Any, Message : Any, Effect, FromState : State, TriggerMessage : Message>(
    val initialState: FromState,
    val message: TriggerMessage
) {
    val effects = ArrayList<Effect>()
}