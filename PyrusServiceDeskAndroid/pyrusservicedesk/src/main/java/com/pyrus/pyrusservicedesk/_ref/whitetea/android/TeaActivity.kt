package com.pyrus.pyrusservicedesk._ref.whitetea.android

import android.os.Bundle
import android.os.PersistableBundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal abstract class TeaActivity<Model : Any, Event : Any, Effect : Any> : FragmentActivity(), TeaView<Model, Event, Effect> {

    private val messagesFlow = MutableSharedFlow<Event>()

    private var renderer: ViewRenderer<Model>? = null

    override val messages: Flow<Event> = messagesFlow

    protected open fun createRenderer(): ViewRenderer<Model>? {
        return null
    }

    fun dispatch(message: Event) {
        lifecycleScope.launch { messagesFlow.emit(message) }
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        renderer = createRenderer()
    }

    override fun render(model: Model) {
        renderer?.render(model)
    }

    override fun handleEffect(effect: Effect) {}
}