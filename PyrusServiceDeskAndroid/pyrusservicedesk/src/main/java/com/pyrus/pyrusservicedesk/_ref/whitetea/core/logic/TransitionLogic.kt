package com.pyrus.pyrusservicedesk._ref.whitetea.core.logic

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Update
import com.pyrus.pyrusservicedesk._ref.whitetea.core.transition.TransitionBuilder
import com.pyrus.pyrusservicedesk._ref.whitetea.core.transition.TransitionSpace
import kotlin.reflect.KClass

internal class TransitionLogic<State : Any, Message : Any, Effect : Any>(
    block: TransitionBuilder<State, Message, Effect>.() -> Unit
) : L<State, Message, Effect> {

    private val stateRoute: Map<KClass<out Message>, Map<KClass<out State>, Nothing.() -> State>>
    private val freeTransitions: Map<KClass<out Message>, TransitionSpace<State, Message, Effect, State, Message>.() -> Unit>

    init {
        val transitionBuilder = TransitionBuilder<State, Message, Effect>()
        transitionBuilder.block()
        val messageMap = HashMap<KClass<out Message>, HashMap<KClass<out State>, Nothing.() -> State>>()
        for (transition in transitionBuilder.transitions) {
            messageMap.getOrPut(transition.trigger) { HashMap() }[transition.fromState] = transition.transition
        }
        stateRoute = messageMap
        freeTransitions = transitionBuilder.freeTransitions
    }

    @Suppress("UNCHECKED_CAST")
    override fun update(message: Message, state: State): Update<State, Effect> {
        val freeTransition = freeTransitions[message::class]
        val action = stateRoute[message::class]?.get(state::class) as? TransitionSpace<State, Message, Effect, State, Message>.() -> State
        if (action != null || freeTransition != null) {
            val space = TransitionSpace<State, Message, Effect, State, Message>(
                state,
                message
            )
            val newState = action?.invoke(space)
            freeTransition?.invoke(space)
            return Update(newState ?: state, space.effects)
        }

        return Update(state, emptyList())
    }

}