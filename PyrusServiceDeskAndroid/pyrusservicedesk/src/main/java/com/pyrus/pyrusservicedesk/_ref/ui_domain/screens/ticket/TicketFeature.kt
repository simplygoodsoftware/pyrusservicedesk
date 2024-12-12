package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments

internal typealias TicketFeature = Store<TicketContract.State, TicketContract.Message, TicketContract.Effect>

internal interface TicketContract {

    data class State(
        val comments: Comments?,
        val isLoading: Boolean,
        val sendEnabled: Boolean,
        val inputText: String,
        val titleText: String,
        val showError: Boolean,
        val welcomeMessage: String?,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            data class OnPreviewClick(val uri: Uri) : Outer

            data class OnRetryClick(val id: Long) : Outer

            data class OnCopyClick(val text: String) : Outer

            data class OnRatingClick(val rating: Int) : Outer

            data object OnShowAttachVariantsClick : Outer

            data object OnSendClick : Outer

            data object OnCloseClick : Outer

            data class OnMessageChanged(val text: String) : Outer

            data class OnAttachmentSelected(val fileUri: Uri?) : Outer

        }

        sealed interface Inner : Message {
            data class CommentsUpdated(val comments: Comments?) : Inner

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
            data object Close : Inner
            data class CopyToClipboard(val text: String) : Inner
            data class SendComment(val text: String) : Inner
            data class OpenPreview(val uri: Uri) : Inner
        }
    }

}