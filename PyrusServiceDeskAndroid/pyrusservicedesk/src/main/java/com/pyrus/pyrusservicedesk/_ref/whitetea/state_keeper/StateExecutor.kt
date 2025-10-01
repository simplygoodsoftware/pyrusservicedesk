package com.pyrus.pyrusservicedesk._ref.whitetea.state_keeper

import android.os.Parcelable
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Executor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Update

internal class StateExecutor<State : Any, Message : Any, Effect : Any>(
    private val delegate: Executor<State, Message, Effect>,
    private val stateKeeper: StateKeeper?,
    private val name: String,
    private val mapper: ((State) -> Parcelable)
): Executor<State, Message, Effect> by delegate {

    override fun init(callbacks: Executor.Callbacks<State, Message, Effect>) {

        delegate.init(object : Executor.Callbacks<State, Message, Effect> {

            override fun onCreateUpdate(message: Message): Update<State, Effect> {
                return callbacks.onCreateUpdate(message)
            }

            override suspend fun onEffect(effect: Effect) {
                callbacks.onEffect(effect)
            }

            override fun onState(state: State) {
                callbacks.onState(state)
                stateKeeper?.prepare(name, mapper(state))
            }

        })
    }
}