package com.pyrus.pyrusservicedesk._ref.whitetea.core

import com.pyrus.pyrusservicedesk._ref.whitetea.utils.Cancelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface Store<State : Any, Message : Any, Effect : Any>: Cancelable {

    val state: StateFlow<State>

    val effects: Flow<Effect>

    fun init()

    fun dispatch(message: Message)

}