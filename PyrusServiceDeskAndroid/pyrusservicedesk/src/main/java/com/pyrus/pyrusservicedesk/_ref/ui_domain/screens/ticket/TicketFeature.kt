package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store

internal typealias TicketFeature = Store<TicketContract.State, TicketContract.Message, TicketContract.Effect>

internal interface TicketContract {

    data class State(
        val comments: List<CommentEntryV2>?,
        val isLoading: Boolean,
        val sendEnabled: Boolean,
        val inputText: String,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            data object OnPreviewClick : Outer

            data object OnRetryClick : Outer

            data class OnCopyClick(val text: String) : Outer

            data class OnRatingClick(val rating: Int) : Outer

            data object OnShowAttachVariantsClick : Outer

            data object OnSendClick : Outer

            data object OnCloseClick : Outer

            data class OnMessageChanged(val text: String) : Outer

            data class OnAttachmentSelected(val fileUri: Uri?) : Outer

        }

        sealed interface Inner : Message {
            data class CommentsUpdated(val comments: List<CommentEntryV2>?) : Inner

            data object UpdateCommentsFailed : Inner
            data object UpdateCommentsCompleted : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect

        sealed interface Inner : Effect {
            data object UpdateComments : Inner
            data object FeedFlow : Inner
            data object CommentsAutoUpdate : Inner
        }
    }

}