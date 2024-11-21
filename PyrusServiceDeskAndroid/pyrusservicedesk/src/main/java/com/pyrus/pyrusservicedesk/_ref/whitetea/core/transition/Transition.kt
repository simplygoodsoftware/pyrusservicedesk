package com.pyrus.pyrusservicedesk._ref.whitetea.core.transition

import kotlin.reflect.KClass

internal class Transition<State : Any, Message : Any, Effect, TriggerMessage : Message, FromState : State, ToState : State>(
    val trigger: KClass<TriggerMessage>,
    val fromState: KClass<FromState>,
    val toState: KClass<ToState>,
    val transition: TransitionSpace<State, Message, Effect, FromState, TriggerMessage>.() -> ToState
)