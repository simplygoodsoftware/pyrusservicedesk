package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.utils.GetTicketsError
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import java.io.File

internal typealias TicketFeature = Store<TicketContract.State, TicketContract.Message, TicketContract.Effect.Outer>

interface TicketContract {

    sealed interface State {
        data object Loading : State
        data object Error : State
        data class Content(
            val ticket: FullTicket?,
            val ticketId: Long,
            val sendEnabled: Boolean,
            val inputText: String,
            val welcomeMessage: String?,
            val isLoading: Boolean,
            val userName: String,
            val recordState: RecordState,
            val pendingRecord: String?,
        ) : State {
            override fun toString(): String {
                return "State(c=${ticket?.comments?.size})"
            }
        }
    }

    sealed interface RecordState {
        data object None : RecordState
        data class Recording(val recordStartTime: Long) : RecordState
        data class HoldRecording(val recordStartTime: Long) : RecordState
        data object PendingRecord : RecordState
    }

    sealed interface Message {

        sealed interface Outer : Message {
            class OnPreviewClick(val commentId: Long, val attachmentId: Long) : Outer
            class OnErrorCommentClick(val localId: Long) : Outer
            class OnCopyClick(val text: String) : Outer
            class OnRatingClick(val rating: Int?, val ratingComment: String?) : Outer
            object OnShowAttachVariantsClick : Outer
            object OnSendClick : Outer
            object OnCloseClick : Outer
            class OnMessageChanged(val text: String) : Outer
            class OnButtonClick(val text: String) : Outer
            object OnRefresh : Outer
            object OnBackClick : Outer
            class OnCancelUploadClick(val localId: Long, val attachmentId: Long) : Outer
            object OnInfoClick : Outer
            object OnStartRecord : Outer
            object OnStopRecord : Outer
            object OnStopEndSendRecord : Outer
            object OnMicShortClicked : Outer
            object OnCancelRecord : Outer
            object OnLockRecord : Outer
            object OnRemovePendingAudioClick : Outer
            data class SetAttachVariant(val key: String, val uri: Any) : Outer
            data class SetErrorCommentResult(val localId: Long, val key: String, val action: Any) : Outer
        }

        sealed interface Inner : Message {
            class CommentsUpdated(val ticket: FullTicket?) : Inner
            class UpdateCommentsFailed(val getTicketsError: GetTicketsError) : Inner
            class UpdateCommentsCompleted(
                val ticket: FullTicket,
                val draft: String,
                val welcomeMessage: String?,
                val userName: String,
            ) : Inner

            class OnAudioRecorded(val file: File) : Inner
            object OnAudioCancelled : Inner
            class OnRecordingProgressUpdated(val recordedSegmentValues: ShortArray) : Inner
            data object Exit : Inner
            data class OnOpenPreview(val fileData: FileData) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            class CopyToClipboard(val text: String) : Outer
            class MakeToast(val text: TextProvider) : Outer
            class ShowAttachVariants(val key: String) : Outer
            class ShowErrorCommentDialog(val localId: Long, val key: String) : Outer
            class ShowInfoBottomSheetFragment(
                val ticketId: Long,
                val userName: String,
                val createData: Long,
            ) : Outer

            class PlayAudio(val audioFile: File, val guid: String?) : Outer
            class UpdateRecordWave(val recordedSegmentValues: ShortArray) : Outer
            object ShowAudioRecordTooltip : Outer
            data object Exit : Outer
            data class OpenPreview(val fileData: FileData) : Outer
            data class OpenRatingComment(val rateUsText: String?) : Outer
        }

        sealed interface Inner : Effect {
            class UpdateComments(
                val force: Boolean,
                val ticketId: Long,
            ) : Inner
            object FeedFlow : Inner
            object CheckAccount : Inner
            object Close : Inner
            class SendTextComment(
                val text: String?,
                val ticketId: Long,
            ) : Inner
            class SendRatingComment(
                val rating: Int?,
                val ratingComment: String?,
                val ticketId: Long,
            ) : Inner
            class OpenPreview(val attachment: Attachment, val userId: String?) : Inner
            class SaveDraft(val draft: String) : Inner
            class ReadTicket(val ticketId: Long) : Inner
            class ListenAttachVariant(val key: String, val uri: Any) : Inner
            class ListenErrorCommentAction(val localId: Long, val key: String, val action: Any) : Inner
            class CancelFileUpload(val localId: Long, val attachmentId: Long) : Inner

            object SubscribeToRecord : Inner
            object SubscribeToRecordProgress : Inner
            object SubscribeToCancelRecord : Inner
            object StartRecord : Inner
            object StopRecord : Inner
            object CancelRecord : Inner
            class SendAudio(val file: String) : Inner
            class DeleteFile(val file: String) : Inner
            class PauseAudioIf(val file: String) : Inner
            object PauseAudio : Inner
        }
    }

}