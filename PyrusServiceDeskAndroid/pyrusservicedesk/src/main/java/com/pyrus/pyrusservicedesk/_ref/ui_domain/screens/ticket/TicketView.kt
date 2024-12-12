package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2

internal interface TicketView {

    data class Model(
        val titleText: String,
        val inputText: String,
        val sendEnabled: Boolean,
        val comments: List<CommentEntryV2>?,
        val isLoading: Boolean,
        val showNoConnectionError: Boolean,
    )

    sealed interface Event {

        data class OnPreviewClick(val uri: Uri) : Event

        data class OnRetryClick(val id: Long) : Event

        data class OnCopyClick(val text: String) : Event

        data class OnRatingClick(val rating: Int) : Event

        data object OnShowAttachVariantsClick : Event

        data object OnSendClick : Event

        data object OnCloseClick : Event

        data class OnMessageChanged(val text: String) : Event

        data class OnAttachmentSelected(val fileUri: Uri?) : Event

    }
}