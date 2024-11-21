package com.pyrus.pyrusservicedesk._ref.whitetea.android

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer

internal abstract class BaseTeaView<Model : Any, Message : Any, Effect : Any> :
    TeaView<Model, Message, Effect> {

    private val messagesFlow = MutableSharedFlow<Message>(0, 1, BufferOverflow.DROP_OLDEST)

    protected open val renderer: ViewRenderer<Model>? = null

    override val messages: Flow<Message> = messagesFlow

    fun dispatch(message: Message) {
        messagesFlow.tryEmit(message)
    }

    override fun handleEffect(effect: Effect) {}

    override fun render(model: Model) {
        renderer?.render(model)
    }
}