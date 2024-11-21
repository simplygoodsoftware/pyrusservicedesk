package com.pyrus.pyrusservicedesk._ref.whitetea.core.logic

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Update

internal interface L<State : Any, Message : Any, Effect : Any> {

    fun update(message: Message, state: State): Update<State, Effect>

}