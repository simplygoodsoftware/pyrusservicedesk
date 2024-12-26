package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store

internal typealias TicketFeature = Store<TicketContract.State, TicketContract.Message, TicketContract.Effect.Outer>

internal interface TicketContract {

    sealed interface State {
        data object Loading : State
        data object Error : State
        data class Content(
            val ticket: FullTicket?,
            val appId: String,
            val userId: String,
            val ticketId: Int,
            val sendEnabled: Boolean,
            val inputText: String,
            val welcomeMessage: String?,
            val isLoading: Boolean,
        ) : State {
            override fun toString(): String {
                return "State(c=${ticket?.comments?.size})"
            }
        }
    }

    sealed interface Message {

        sealed interface Outer : Message {

            data class OnPreviewClick(val commentId: Long, val attachmentId: Int) : Outer

            data class OnRetryAddCommentClick(val id: Long) : Outer

            data class OnCopyClick(val text: String) : Outer

            data class OnRatingClick(val rating: Int) : Outer

            data object OnShowAttachVariantsClick : Outer

            data object OnSendClick : Outer

            data object OnCloseClick : Outer

            data class OnMessageChanged(val text: String) : Outer

            data class OnAttachmentSelected(val fileUri: Uri?) : Outer

            data object OnRefresh : Outer

            data object OnBackClick : Outer

        }

        sealed interface Inner : Message {
            data class CommentsUpdated(val ticket: FullTicket?) : Inner
            data object UpdateCommentsFailed : Inner
            data class UpdateCommentsCompleted(
                val ticket: FullTicket,
                val draft: String,
                val welcomeMessage: String?,
            ) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            data class CopyToClipboard(val text: String) : Outer
            data class MakeToast(val text: TextProvider) : Outer
            data object ShowAttachVariants : Outer
        }

        sealed interface Inner : Effect {
            data class UpdateComments(
                val ticketId: Int,
                val userId: String,
            ) : Inner
            data object FeedFlow : Inner
            data object Close : Inner
            data class SendTextComment(
                val text: String,
                val ticketId: Int,
                val appId: String,
                val userId: String
            ) : Inner
            data class SendRatingComment(
                val rating: Int,
                val ticketId: Int,
                val appId: String,
                val userId: String
            ) : Inner
            data class SendAttachComment(
                val uri: Uri,
                val ticketId: Int,
                val appId: String,
                val userId: String
            ) : Inner
            data class RetryAddComment(val id: Long) : Inner
            data class OpenPreview(val attachment: Attachment) : Inner
            data class SaveDraft(val draft: String) : Inner
        }
    }

}