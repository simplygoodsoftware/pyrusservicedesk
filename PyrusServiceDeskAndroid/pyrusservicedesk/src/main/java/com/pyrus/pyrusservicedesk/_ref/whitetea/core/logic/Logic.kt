package com.pyrus.pyrusservicedesk._ref.whitetea.core.logic

import com.pyrus.pyrusservicedesk._ref.whitetea.core.UpdateBuilder

internal abstract class Logic<State : Any, Message : Any, Effect : Any> : L<State, Message, Effect> {

    // Needed to type less code
    inner class Result(state: State) : UpdateBuilder<State, Message, Effect>(state)

    protected abstract fun Result.update(message: Message)

    override fun update(message: Message, state: State) = Result(state).apply { update(message) }.build()
}

