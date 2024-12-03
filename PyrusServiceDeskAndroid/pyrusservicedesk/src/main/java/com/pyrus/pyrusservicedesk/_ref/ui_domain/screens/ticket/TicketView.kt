package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry

internal interface TicketView {

    data class TicketModel(
        val titleText: String,
        val inputText: String,
        val sendEnabled: Boolean,
        val comments: List<TicketEntry>,
    )

    sealed interface Event {

        object OnPreviewClick : Event

        object OnRetryClick : Event

        data class OnCopyClick(val text: String) : Event

        data class OnRatingClick(val rating: Int) : Event

        object OnShowAttachVariantsClick : Event

        object OnSendClick : Event

        object OnCloseClick : Event

        data class OnMessageChanged(val text: String) : Event

        data class OnAttachmentClick(val fileUri: Uri?) : Event

    }
}