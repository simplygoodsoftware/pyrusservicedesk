package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider

internal interface TicketView {

    data class Model(
        val inputText: String,
        val sendEnabled: Boolean,
        val comments: List<CommentEntryV2>?,
        val isLoading: Boolean,
        val showNoConnectionError: Boolean,
        val isRefreshing: Boolean,
        val toolbarTitleText: TextProvider?,
    ) {
        override fun toString(): String {
            return "Model(inputText='$inputText', sendEnabled=$sendEnabled, comments=${comments?.size}, isLoading=$isLoading, showNoConnectionError=$showNoConnectionError)"
        }
    }

    sealed interface Event {

        data class OnPreviewClick(val commentId: Long, val attachmentId: Long) : Event

        data class OnErrorCommentClick(val localId: Long) : Event

        data class OnCopyClick(val text: String) : Event

        data class OnRatingClick(val rating: Int) : Event

        data object OnShowAttachVariantsClick : Event

        data object OnSendClick : Event

        data object OnCloseClick : Event

        data object OnBackClick : Event

        data class OnMessageChanged(val text: String) : Event

        data class OnButtonClick(val buttonText: String) : Event

        data object OnRefresh : Event

    }

    sealed interface Effect {
        data class CopyToClipboard(val text: String) : Effect
        data class MakeToast(val text: TextProvider) : Effect
        data class ShowAttachVariants(val key: String) : Effect
        data class ShowErrorCommentDialog(val key: String) : Effect
    }
}