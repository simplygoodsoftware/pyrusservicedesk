package com.pyrus.pyrusservicedesk._ref.whitetea.android

import android.util.Log
import androidx.fragment.app.Fragment
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal abstract class TeaFragment<Model : Any, Event : Any, Effect : Any> : Fragment(), TeaView<Model, Event, Effect> {

    private val messagesFlow = MutableSharedFlow<Event>(0, 1, BufferOverflow.DROP_OLDEST)

    protected open val renderer: ViewRenderer<Model>? = null

    override val messages: Flow<Event> = messagesFlow

    fun dispatch(message: Event) {
        messagesFlow.tryEmit(message)
    }

    override fun render(model: Model) {
        Log.d("SDS", "model: $model")
        renderer?.render(model)
    }

    override fun handleEffect(effect: Effect) {}
}