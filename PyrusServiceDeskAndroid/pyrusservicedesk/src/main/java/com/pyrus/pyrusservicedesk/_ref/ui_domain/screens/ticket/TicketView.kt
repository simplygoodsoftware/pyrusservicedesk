package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.TicketEntry

internal interface TicketView {

    data class TicketModel(
        val titleText: String,
        val inputText: String,
        val sendEnabled: Boolean,
        val comments: List<TicketEntry>,
    )

    sealed interface Event {

        data object OnPreviewClick : Event

        data object OnRetryClick : Event

        data class OnCopyClick(val text: String) : Event

        data class OnRatingClick(val rating: Int) : Event

        data object OnShowAttachVariantsClick : Event

        data object OnSendClick : Event

        data object OnCloseClick : Event

        data class OnMessageChanged(val text: String) : Event

        data class OnAttachmentClick(val fileUri: Uri?) : Event

    }
}