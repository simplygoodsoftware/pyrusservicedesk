package com.pyrus.pyrusservicedesk._ref.whitetea.core

internal interface ExecutorFactory {

    fun <State : Any, Message : Any, Effect : Any> create(
        name: String,
        actors: List<Actor<Effect, Message>>
    ): Executor<State, Message, Effect>

}