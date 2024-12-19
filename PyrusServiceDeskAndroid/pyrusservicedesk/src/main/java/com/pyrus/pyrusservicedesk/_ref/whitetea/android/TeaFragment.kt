package com.pyrus.pyrusservicedesk._ref.whitetea.android

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal abstract class TeaFragment<Model : Any, Event : Any, Effect : Any> : Fragment(), TeaView<Model, Event, Effect> {

    private val messagesFlow = MutableSharedFlow<Event>(0, 1, BufferOverflow.DROP_OLDEST)

    private var renderer: ViewRenderer<Model>? = null

    override val messages: Flow<Event> = messagesFlow

    protected open fun createRenderer(): ViewRenderer<Model>? {
        return null
    }

    fun dispatch(message: Event) {
        messagesFlow.tryEmit(message)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderer = createRenderer()
    }

    override fun render(model: Model) {
        Log.d("SDS", "model: $model")
        renderer?.render(model)
    }

    override fun handleEffect(effect: Effect) {}
}