package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model

internal object TicketMapper {

    fun map(state: State) = Model(
        titleText = state.titleText,
        inputText = state.inputText,
        sendEnabled = state.sendEnabled,
        comments = state.comments,
        isLoading = state.isLoading,
        showNoConnectionError =  state.showError,
    )

    fun map(event: Event): Message.Outer = when(event) {
        is Event.OnAttachmentSelected -> Message.Outer.OnAttachmentSelected(event.fileUri)
        Event.OnCloseClick -> Message.Outer.OnCloseClick
        is Event.OnCopyClick -> Message.Outer.OnCopyClick(event.text)
        is Event.OnMessageChanged -> Message.Outer.OnMessageChanged(event.text)
        Event.OnPreviewClick -> Message.Outer.OnPreviewClick
        is Event.OnRatingClick -> Message.Outer.OnRatingClick(event.rating)
        Event.OnRetryClick -> Message.Outer.OnRetryClick
        Event.OnSendClick -> Message.Outer.OnSendClick
        Event.OnShowAttachVariantsClick -> Message.Outer.OnShowAttachVariantsClick
    }

}