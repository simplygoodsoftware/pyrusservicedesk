package com.pyrus.pyrusservicedesk._ref.whitetea.android

import androidx.fragment.app.Fragment
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal abstract class TeaFragment<State : Any, Message : Any, Effect : Any>: Fragment(),
    TeaView<State, Message, Effect> {

    private val messagesFlow = MutableSharedFlow<Message>(0, 1, BufferOverflow.DROP_OLDEST)

    override val messages: Flow<Message> = messagesFlow

    fun dispatch(message: Message) {
        messagesFlow.tryEmit(message)
    }

    override fun handleEffect(effect: Effect) {}
}