package com.pyrus.pyrusservicedesk._ref.whitetea.android

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

abstract class TeaFragment<Model : Any, Event : Any, Effect : Any> : Fragment(), TeaView<Model, Event, Effect> {

    private val messagesFlow = MutableSharedFlow<Event>()

    private var renderer: ViewRenderer<Model>? = null

    override val messages: Flow<Event> = messagesFlow

    private val isFirst = AtomicBoolean(true)

    protected open fun createRenderer(): ViewRenderer<Model>? {
        return null
    }

    fun dispatch(message: Event) {
        lifecycleScope.launch { messagesFlow.emit(message) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFirst.set(true)
        renderer = createRenderer()
    }

    override fun render(model: Model) {
        renderer?.render(model)
    }

    override fun handleEffect(effect: Effect) {}

    fun <T> Flow<T>.debounceSkipFirst(
        timeout: Long,
    ): Flow<T> {

        return this.debounce {
            if (isFirst.getAndSet(false)) 0 else timeout
        }
    }
}