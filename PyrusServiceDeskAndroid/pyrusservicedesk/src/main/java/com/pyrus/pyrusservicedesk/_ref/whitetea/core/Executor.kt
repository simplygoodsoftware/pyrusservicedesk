package com.pyrus.pyrusservicedesk._ref.whitetea.core

import androidx.annotation.MainThread

internal interface Executor<State : Any, Message : Any, Effect : Any> {

    fun init(callbacks: Callbacks<State, Message, Effect>)

    fun handleEffect(effect: Effect)

    fun executeMessage(message: Message)

    fun cancel()

    interface Callbacks<State : Any, Message: Any, Effect: Any> {

        fun onCreateUpdate(message: Message): Update<State, Effect>

        @MainThread
        suspend fun onEffect(effect: Effect)

        @MainThread
        fun onState(state: State)

    }

}