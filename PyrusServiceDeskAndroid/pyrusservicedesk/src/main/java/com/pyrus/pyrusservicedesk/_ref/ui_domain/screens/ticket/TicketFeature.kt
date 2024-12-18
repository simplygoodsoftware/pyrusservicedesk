package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.sdk.data.Ticket

internal typealias TicketFeature = Store<TicketContract.State, TicketContract.Message, TicketContract.Effect.Outer>

internal interface TicketContract {

    sealed interface State {
        data object Loading : State
        data object Error : State
        data class Content(
            val ticket: Ticket?,
            val appId: String,
            val userId: String,
            val ticketId: Int,
            val sendEnabled: Boolean,
            val inputText: String,
            val welcomeMessage: String?,
        ) : State {
            override fun toString(): String {
                return "State(c=${ticket?.comments?.size})"
            }
        }
    }

    sealed interface Message {

        sealed interface Outer : Message {

            data class OnPreviewClick(val uri: Uri) : Outer

            data class OnRetryClick(val id: Long) : Outer

            data class OnCopyClick(val text: String) : Outer

            data class OnRatingClick(val rating: Int) : Outer

            data object OnShowAttachVariantsClick : Outer

            data class OnSendClick(
                val ticketId: Int,
                val appId: String,
                val userId: String
            ) : Outer

            data object OnCloseClick : Outer

            data object OnBackClick : Outer

            data class OnMessageChanged(val text: String) : Outer

            data class OnAttachmentSelected(val fileUri: Uri?) : Outer

        }

        sealed interface Inner : Message {
            data class CommentsUpdated(val ticket: Ticket?) : Inner
            data object UpdateCommentsFailed : Inner
            data class UpdateCommentsCompleted(
                val ticket: Ticket?,
                val draft: String,
                val welcomeMessage: String?,
            ) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            data class CopyToClipboard(val text: String) : Outer
            data class MakeToast(val text: TextProvider) : Outer
        }

        sealed interface Inner : Effect {
            data class UpdateComments(val ticketId: Int = 0) : Inner
            data object FeedFlow : Inner
            data object CommentsAutoUpdate : Inner
            data object Close : Inner
            data class SendTextComment(
                val text: String,
                val ticketId: Int,
                val appId: String,
                val userId: String
            ) : Inner
            data class SendAttachComment(val uri: Uri) : Inner
            data class OpenPreview(val uri: Uri) : Inner
            data class SaveDraft(val draft: String) : Inner
        }
    }

}