package com.pyrus.pyrusservicedesk._ref.whitetea.core.transition

import kotlin.reflect.KClass

internal class TransitionBuilder<State : Any, Message : Any, Effect> {
    internal val transitions = ArrayList<Transition<State, Message, Effect, out Message, out State, out State>>()
    internal val freeTransitions = HashMap<KClass<out Message>, TransitionSpace<State, Message, Effect, State, out Message>.() -> Unit>()

    fun <TriggerMessage : Message, FromState : State, ToState : State> add(
        trigger: KClass<TriggerMessage>,
        fromState: KClass<FromState>,
        toState: KClass<ToState>,
        transition: TransitionSpace<State, Message, Effect, FromState, TriggerMessage>.() -> ToState
    ) {
        transitions.add(Transition(trigger, fromState, toState, transition))
    }

    @Suppress("UNCHECKED_CAST")
    fun <TriggerMessage : Message> addFree(
        trigger: KClass<TriggerMessage>,
        transition: TransitionSpace<State, Message, Effect, Nothing, TriggerMessage>.() -> Unit
    ) {
        freeTransitions[trigger] = transition as TransitionSpace<State, Message, Effect, State, out Message>.() -> Unit
    }

    infix fun <TriggerMessage : Message> KClass<TriggerMessage>.free(
        transition: TransitionSpace<State, Message, Effect, Nothing, TriggerMessage>.() -> Unit
    ) = addFree(this, transition)

    infix fun <TriggerMessage : Message, FromState : State>
            KClass<TriggerMessage>.from(that: KClass<FromState>) = TriggerTo(this, that)

    inline infix fun <TriggerMessage : Message, FromState : State, reified ToState : State>
        TriggerTo<KClass<TriggerMessage>, KClass<FromState>>.toState(
        noinline transition: TransitionSpace<State, Message, Effect, FromState, TriggerMessage>.() -> ToState
    ) = add(trigger, fromState, ToState::class, transition)

    data class TriggerTo<out TriggerMessage, out FromState>(
        val trigger: TriggerMessage,
        val fromState: FromState,
    )

}