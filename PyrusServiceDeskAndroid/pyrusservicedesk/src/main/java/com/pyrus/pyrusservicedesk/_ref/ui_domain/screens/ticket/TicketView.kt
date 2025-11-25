package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import java.io.File

internal interface TicketView {

    data class Model(
        val inputText: String,
        val sendEnabled: Boolean,
        val comments: List<CommentEntry>?,
        val isLoading: Boolean,
        val showNoConnectionError: Boolean,
        val isRefreshing: Boolean,
        val toolbarTitleText: TextProvider?,
        val showInputPanel: Boolean,
        val wavesIsVisible: Boolean,
        val recordState: TicketContract.RecordState,
        val pendingAudio: String?,
        val actionButtonIsSend: Boolean,
        val canDragRecordMic: Boolean,
        val scrollDownIsVisible: Boolean,
        val showRating: Boolean,
        val ratingTextRvVisibility: Boolean,
        val smileLl5Visibility: Boolean,
        val smileLlVisibility: Boolean,
        val likeLlVisibility: Boolean,
        val rating2MiniVisibility: Boolean,
        val ratingText: String?,
        val size: Int?,
        val type: Int?,
        val  ratingTextValues: List<CommentEntry.RatingTextValues>?,
    ) {
        override fun toString(): String {
            return "Model(inputText='$inputText', sendEnabled=$sendEnabled, comments=${comments?.size}, isLoading=$isLoading, showNoConnectionError=$showNoConnectionError)"
        }
    }

    sealed interface Event {
        data class OnPreviewClick(val commentId: Long, val attachmentId: Long) : Event
        data class OnErrorCommentClick(val localId: Long) : Event
        data class OnCopyClick(val text: String) : Event
        data class OnRatingClick(val rating: Int?, val rateUsText: String?) : Event
        data object OnShowAttachVariantsClick : Event
        data object OnSendClick : Event
        data object OnCloseClick : Event
        data object OnBackClick : Event
        data class OnMessageChanged(val text: String) : Event
        data class OnButtonClick(val buttonText: String) : Event
        data object OnRefresh : Event
        data class OnCancelUploadClick(val localId: Long, val attachmentId: Long) : Event
        data object OnInfoClick : Event
        data class SetAttachVariant(val key: String, val uri: Any) : Event

        object OnStopRecord : Event
        object OnStopEndSendRecord : Event
        object OnMicShortClicked : Event
        object OnStartRecord : Event
        object OnCancelRecord : Event
        object OnLockRecord : Event
        object OnRemovePendingAudioClick : Event

        data class SetErrorCommentResult(val localId: Long, val key: String, val action: Any) : Event
    }

    sealed interface Effect {
        data class CopyToClipboard(val text: String) : Effect
        data class MakeToast(val text: TextProvider) : Effect
        data class ShowAttachVariants(val key: String) : Effect
        data class ShowErrorCommentDialog(val key: String, val localId: Long) : Effect
        data class ShowInfoBottomSheetFragment(
            val ticketId: Long,
            val userName: String,
            val createData: Long,
        ) : Effect
        data class PlayAudio(val audioFile: File, val guid: String?) : Effect
        class UpdateRecordWave(val recordedSegmentValues: ShortArray) : Effect
        object ShowAudioRecordTooltip : Effect
        data object Exit : Effect
        data class OpenPreview(val fileData: FileData) : Effect
        data class OpenRatingComment(val rateUsText: String?) : Effect
    }
}