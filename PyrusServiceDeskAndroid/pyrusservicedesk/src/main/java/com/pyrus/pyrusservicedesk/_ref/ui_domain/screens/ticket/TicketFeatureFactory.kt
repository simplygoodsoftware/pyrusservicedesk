package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.*
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message.*
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Update
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.L
import kotlinx.coroutines.flow.Flow

internal class TicketFeatureFactory(
    private val storeFactory: StoreFactory2,
    private val actor: TicketActor,
) {

    fun create(): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State(),
        reducer = FeatureReducer(),
        actor = actor,
    )

}

private class FeatureReducer: L<State, Message, Effect>  {

    override fun update(message: Message, state: State): Update<State, Effect> = when(message) {
        is Outer -> handleOuter(message)
        is Inner -> handleInner(message)
    }

    private fun handleOuter(message: Outer): Update<State, Effect> = when (message) {
        is Outer.OnAttachmentSelected -> TODO("send comment with attachment")
        Outer.OnCloseClick -> TODO("close screen")
        is Outer.OnCopyClick -> TODO("copy text to copy buffer")
        is Outer.OnMessageChanged -> TODO("save to state")
        Outer.OnPreviewClick -> TODO("show preview screen")
        is Outer.OnRatingClick -> TODO("send rating comment")
        Outer.OnRetryClick -> TODO("retry send comment")
        Outer.OnSendClick -> TODO("send current")
        Outer.OnShowAttachVariantsClick -> TODO("open attach variants dialog screen")
    }

    private fun handleInner(message: Inner): Update<State, Effect> {
        TODO()
//        when (message) {
//
//        }
    }

}

internal class TicketActor: Actor<Effect, Message> {

    override fun handleEffect(effect: Effect): Flow<Message> {
        TODO("Not yet implemented")
    }

}