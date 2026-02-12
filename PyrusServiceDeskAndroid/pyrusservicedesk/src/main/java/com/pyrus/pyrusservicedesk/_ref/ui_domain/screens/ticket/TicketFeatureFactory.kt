package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_1
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.RecordState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record.AudioRecordControllerFactory
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.GetTicketsError
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getFileUrl
import com.pyrus.pyrusservicedesk._ref.utils.Try2
import com.pyrus.pyrusservicedesk._ref.utils.isAudio
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.record.AudioRecordController
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.core.getVersion
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.ErrorCommentActionsDialog.Companion.ErrorCommentAction
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

internal class TicketFeatureFactory(
    private val accountStore: AccountStore,
    private val storeFactory: StoreFactory,
    private val repository: SdRepository,
    private val draftRepository: DraftRepository,
    private val router: PyrusRouter,
    private val fileManager: FileManager,
    private val preferencesManager: PreferencesManager,
    private val audioRecordControllerFactory: AudioRecordControllerFactory,
    private val audioWrapper: AudioWrapper,
    private val localTicketsStore: LocalTicketsStore,
    private val commandsStore: LocalCommandsStore,
    private val idStore: IdStore
) {

    fun create(
        user: UserInternal,
        initialTicketId: Long,
        welcomeMessage: String?,
        sendComment: String?,
    ): TicketFeature {
        val audioRecordController = audioRecordControllerFactory.create()
        return storeFactory.create(
            name = "TicketFeature",
            initialState = State.Loading,
            reducer = FeatureReducer(),
            actor = TicketActor(
                accountStore = accountStore,
                ticketId = initialTicketId,
                user = user,
                repository = repository,
                router = router,
                welcomeMessage = welcomeMessage,
                draftRepository = draftRepository,
                fileManager = fileManager,
                preferencesManager = preferencesManager,
                audioRecordController = audioRecordController,
                audioWrapper = audioWrapper,
                localTicketsStore = localTicketsStore,
                commandsStore = commandsStore,
                idStore = idStore,
            ).adaptCast(),
            initialEffects = listOf(
                Effect.Inner.FeedFlow,
                Effect.Inner.UpdateComments(force = false, ticketId = initialTicketId),
                Effect.Inner.ReadTicketIfNeed(ticketId = initialTicketId),
                Effect.Inner.CheckAccount,
                Effect.Inner.SubscribeToRecord,
                Effect.Inner.SubscribeToRecordProgress,
                Effect.Inner.SubscribeToCancelRecord,
                Effect.Inner.SendTextCommentIfIsNotNullOrBlank(sendComment, initialTicketId),
                Effect.Inner.UpdateAudioData
            ),
            onCancelCallback = {
                audioRecordController.cancelRecord()
            }
        ).adapt { it as? Effect.Outer }
    }

}

private class FeatureReducer(): Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
        val currentState = state
        if (currentState is State.Content) {
            state { currentState.copy(sendEnabled = currentState.inputText.isNotBlank() || currentState.pendingRecord != null) }
        }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            is Message.Outer.OnCloseClick -> effects { +Effect.Inner.Close }
            is Message.Outer.OnBackClick -> effects { +Effect.Inner.Close }
            is Message.Outer.OnCopyClick -> effects {
                +Effect.Outer.CopyToClipboard(message.text)
                +Effect.Outer.MakeToast(R.string.psd_copied_to_clipboard.textRes())
            }
            is Message.Outer.OnMessageChanged -> {
                val currentState = state as? State.Content ?: return
                state { currentState.copy(inputText = message.text) }
                effects { +Effect.Inner.SaveDraft(message.text) }
            }
            is Message.Outer.OnButtonClick -> {
                val currentState = state as? State.Content ?: return
                val buttonComment = message.text
                if (buttonComment.isBlank()) return
                effects { +Effect.Inner.SendTextCommentIfIsNotNullOrBlank(buttonComment, currentState.ticketId) }
            }
            is Message.Outer.OnPreviewClick -> {
                val currentState = state as? State.Content ?: return
                val attach = currentState.ticket?.comments
                    ?.find { it.id == message.commentId }?.attachments
                    ?.find { it.id == message.attachmentId } ?: return

                effects { +Effect.Inner.OpenPreview(attach, currentState.ticket.userId) }
            }
            is Message.Outer.OnRatingClick -> {
                val currentState = state as? State.Content ?: return
                val ticket = currentState.ticket
                state { currentState.copy(ticket = ticket?.copy(showRating = false)) }
                effects { +Effect.Inner.SendRatingComment(
                    message.rating,
                    message.ratingComment,
                    ticketId = (state as State.Content).ticketId,
                ) }
                if (message.ratingComment == null)
                    effects { +Effect.Outer.OpenRatingComment(currentState.ticket?.showRatingText) }
            }
            is Message.Outer.OnErrorCommentClick -> {
                if (state !is State.Content) return
                effects {
                    val key = UUID.randomUUID().toString()
                    +Effect.Outer.ShowErrorCommentDialog(message.localId, key)
                }
            }
            is Message.Outer.OnSendClick -> {
                val currentState = state as? State.Content ?: return
                val pendingRecord = currentState.pendingRecord
                if (pendingRecord != null) {
                    effects { +Effect.Inner.PauseAudioIf(pendingRecord) }
                    effects { +Effect.Inner.SendAudio(pendingRecord) }
                    state { currentState.copy(recordState = RecordState.None, pendingRecord = null) }
                    return
                }
                val comment = currentState.inputText
                if (comment.isBlank()) return
                effects { +Effect.Inner.SendTextCommentIfIsNotNullOrBlank(comment, currentState.ticketId) }
            }
            is Message.Outer.OnShowAttachVariantsClick -> {
                if(state !is State.Content) return
                effects {
                    val key = UUID.randomUUID().toString()
                    +Effect.Outer.ShowAttachVariants(key)
                }
            }
            is Message.Outer.OnRefresh -> {
                val currentState = state as? State.Content ?: return
                if (currentState.isLoading) return
                state { currentState.copy(isLoading = true) }
                effects { +Effect.Inner.UpdateComments(
                    force = true,
                    ticketId = currentState.ticketId,
                ) }
            }
            is Message.Outer.OnCancelUploadClick -> {
                if (state !is State.Content) return
                effects { +Effect.Inner.CancelFileUpload(message.localId, message.attachmentId) }
            }

            is Message.Outer.OnInfoClick -> {
                val currentState = state as? State.Content ?: return
                effects {
                    +Effect.Outer.ShowInfoBottomSheetFragment(
                        currentState.ticket?.ticketId ?: -1,
                        currentState.userName,
                        currentState.ticket?.comments?.first()?.creationTime ?: -1
                    )
                }
            }
            is Message.Outer.OnStartRecord -> {
                Log.d("DFD", "OnStartRecord")
                val currentState = state as? State.Content ?: return
                effects { +Effect.Inner.StartRecord }
                effects { +Effect.Inner.PauseAudio }
                state { currentState.copy(recordState = RecordState.Recording(System.currentTimeMillis())) }
            }
            is Message.Outer.OnStopRecord -> {
                Log.d("DFD", "OnStopRecord")
                val currentState = state as? State.Content ?: return
                if (currentState.recordState is RecordState.HoldRecording) {
                    state { currentState.copy(recordState = RecordState.PendingRecord) }
                }
                else {
                    state { currentState.copy(recordState = RecordState.None) }
                }
                effects { +Effect.Inner.StopRecord }

            }
            is Message.Outer.OnStopEndSendRecord -> {
                Log.d("DFD", "OnStopEndSendRecord")
                val currentState = state as? State.Content ?: return
                state { currentState.copy(recordState = RecordState.None) }
                effects { +Effect.Inner.StopRecord }
            }
            is Message.Outer.OnMicShortClicked -> {
                Log.d("DFD", "OnMicShortClicked")
                state as? State.Content ?: return
                effects { +Effect.Outer.ShowAudioRecordTooltip }
            }
            is Message.Outer.OnCancelRecord -> {
                Log.d("DFD", "OnCancelRecord")
                val currentState = state as? State.Content ?: return
                state { currentState.copy(recordState = RecordState.None) }
                effects { +Effect.Inner.CancelRecord }
            }
            is Message.Outer.OnLockRecord -> {
                Log.d("DFD", "OnLockRecord")
                val currentState = state as? State.Content ?: return
                val recordState = currentState.recordState as? RecordState.Recording ?: return
                state { currentState.copy(recordState = RecordState.HoldRecording(recordState.recordStartTime)) }
            }
            is Message.Outer.OnRemovePendingAudioClick -> {
                Log.d("DFD", "OnRemovePendingAudioClick")
                val currentState = state as? State.Content ?: return
                currentState.pendingRecord?.let {
                    effects { +Effect.Inner.PauseAudioIf(it) }
                    effects { +Effect.Inner.DeleteFile(it) }
                }
                state { currentState.copy(recordState = RecordState.None, pendingRecord = null) }
            }

            is Message.Outer.SetAttachVariant -> effects {
                +Effect.Inner.ListenAttachVariant(message.key, message.uri)
            }

            is Message.Outer.SetErrorCommentResult -> effects {
                +Effect.Inner.ListenErrorCommentAction(message.localId, message.key, message.action)
            }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            is Message.Inner.UpdateCommentsFailed -> {
                when (val currentState = state) {
                    is State.Content -> {
                        when (message.getTicketsError) {
                            GetTicketsError.NoDataFound -> effects { +Effect.Inner.Close }
                            GetTicketsError.AuthorAccessDenied -> effects { +Effect.Inner.Close }
                            else -> state { currentState.copy(isLoading = false) }
                        }
                    }
                    is State.Loading -> state { State.Error }
                    State.Error -> {}
                }
            }
            is Message.Inner.UpdateCommentsCompleted -> {
                when(val currentState = state) {
                    is State.Content -> state { currentState.copy(
                        ticket = message.ticket,
                        isLoading = false,
                        ticketId = message.ticket.ticketId,
                        welcomeMessage = message.welcomeMessage
                    ) }
                    State.Error,
                    State.Loading,
                        -> state {
                        var welcomeMessage = message.welcomeMessage
                        if (message.ticket.comments.isNotEmpty() && message.ticket.comments.first().isSupport)
                            welcomeMessage = null
                        State.Content(
                            ticket = message.ticket,
                            sendEnabled = true,
                            inputText = message.draft,
                            welcomeMessage = welcomeMessage,
                            isLoading = false,
                            ticketId = message.ticket.ticketId,
                            userName = message.userName,
                            recordState = RecordState.None,
                            pendingRecord = null,
                            voiceMessage = ConfigUtils.getVoiceMessage(),
                            previousTicketLastCommentId = message.previousTicketLastCommentId,
                        )

                    }
                }
                val ticket = message.ticket
                if (!ticket.isRead) {
                    effects { +Effect.Inner.ReadTicketIfNeed(ticket.ticketId) }
                }
            }
            is Message.Inner.CommentsUpdated -> {
                val currentState = state as? State.Content ?: return

                val ticket = message.ticket
                if (ticket != null && !ticket.isRead) {
                    effects { +Effect.Inner.ReadTicketIfNeed(ticket.ticketId) }
                }
                var welcomeMessage = if (ticket?.welcomeMessage.isNullOrBlank()) message.welcomeMessage else ticket.welcomeMessage
                if (!ticket?.comments.isNullOrEmpty() && message.ticket.comments.first().isSupport)
                    welcomeMessage = null
                state { currentState.copy(
                    welcomeMessage = welcomeMessage,
                    ticket = message.ticket
                ) }
            }
            is Message.Inner.OnAudioRecorded -> {
                Log.d("DFD", "message OnAudioRecorded")
                val currentState = state as? State.Content ?: return
                if (currentState.recordState == RecordState.PendingRecord) {
                    state { currentState.copy(pendingRecord = message.file.path) }
                }
                else {
                    effects { +Effect.Inner.SendAudio(message.file.path) }
                }

            }
            is Message.Inner.OnAudioCancelled -> {
                Log.d("DFD", "message Inner.OnAudioCancelled")
                val currentState = state as? State.Content ?: return
                when(currentState.recordState) {
                    is RecordState.HoldRecording,
                    is RecordState.PendingRecord,
                    is RecordState.Recording,
                        -> {
                        state { currentState.copy(
                            recordState = RecordState.None,
                        ) }
                    }
                    RecordState.None -> {}
                }
            }
            is Message.Inner.OnRecordingProgressUpdated -> {
                effects { +Effect.Outer.UpdateRecordWave(message.recordedSegmentValues) }
            }
            is Message.Inner.ShowToast -> effects { +Effect.Outer.MakeToast(message.message) }

            Message.Inner.Exit -> effects { +Effect.Outer.Exit }
            is Message.Inner.OnOpenPreview -> effects { +Effect.Outer.OpenPreview(message.fileData) }
        }
    }

}

@OptIn(FlowPreview::class)
private class TicketActor(
    private val accountStore: AccountStore,
    private var ticketId: Long,
    private val user: UserInternal,
    private val repository: SdRepository,
    private val router: PyrusRouter,
    private val welcomeMessage: String?,
    private val draftRepository: DraftRepository,
    private val fileManager: FileManager,
    private val preferencesManager: PreferencesManager,
    private val audioRecordController: AudioRecordController,
    private val audioWrapper: AudioWrapper,
    private val localTicketsStore: LocalTicketsStore,
    private val commandsStore: LocalCommandsStore,
    private val idStore: IdStore,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when (effect) {
        is Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try2<FullTicket, GetTicketsError> = repository.getFeed(
                userId = user.userId,
                ticketId = effect.ticketId,
                force = effect.force
            )
            when {
                commentsTry.isSuccess() -> {
                    val application = localTicketsStore.getApplications().find { accountStore.getAccount().getUsers().find { user -> user.userId == commentsTry.value.userId }?.appId == it.appId }
                    var needAnotherOneWelcomeMessage = false
                    if (((accountStore.getAccount().getVersion() == API_VERSION_1
                            ||accountStore.getAccount().getVersion() == API_VERSION_2)
                            && localTicketsStore.getTickets().lastOrNull()?.isActive == false) && !commentsTry.value.showRating
                    ) {
                            needAnotherOneWelcomeMessage = true
                    }
                    Message.Inner.UpdateCommentsCompleted(
                        ticket = commentsTry.value,
                        draft = draftRepository.getDraft(ticketId),
                        welcomeMessage = if (application?.welcomeMessage.isNullOrBlank()) welcomeMessage else application?.welcomeMessage,
                        userName = accountStore.getAccount().getUsers()
                            .find { it.userId == commentsTry.value.userId }?.userName ?: "",
                        previousTicketLastCommentId = if (needAnotherOneWelcomeMessage) commentsTry.value.comments.lastOrNull()?.id else null,
                    )
                }
                else -> Message.Inner.UpdateCommentsFailed(commentsTry.error)
            }
        }
        is Effect.Inner.FeedFlow -> {
            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            repository.getFeedFlowByTicketIdFlow(user, idStore.ticketIdFlow)
                .map { Message.Inner.CommentsUpdated(it, welcomeMessage) }
        }
        is Effect.Inner.CheckAccount -> flow {
            var oldAccount: Account? = null
            accountStore.accountStateFlow().collect { account ->
                val users = account.getUsers()
                if (!users.any { user.userId == it.userId && user.appId == it.appId }) {
                    emit(Message.Inner.Exit) //router.exit() //TODO kate think about router
                }
                oldAccount?.let {
                    if (it.getUsers().size < account.getUsers().size) {
                        emit(Message.Inner.Exit)
                    }
                }
                oldAccount = account
            }
        }
        is Effect.Inner.Close -> singleFlow { Message.Inner.Exit }
        is Effect.Inner.SendTextCommentIfIsNotNullOrBlank -> flow {
            //TODO for multichat
            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            if (effect.text.isNullOrBlank())
                return@flow
            preferencesManager.saveLastActiveTime(System.currentTimeMillis())
            repository.addTextComment(user, ticketId, effect.text)
        }
        is Effect.Inner.SendRatingComment -> flow {
            preferencesManager.saveLastActiveTime(System.currentTimeMillis())
            if (effect.rating == null && effect.ratingComment == null)
                return@flow
            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            repository.addRatingComment(user, ticketId, effect.rating, effect.ratingComment)
        }
        is Effect.Inner.OpenPreview -> singleFlow {
            val user = (accountStore.getAccount() as? Account.V3)?.users?.find { it.userId == effect.userId }
            val fileData = FileData(
                fileName = effect.attachment.name,
                bytesSize = effect.attachment.bytesSize,
                uri = getFileUrl(effect.attachment.id, accountStore.getAccount(), user).toUri(),
                isLocal = false,
            )
            Message.Inner.OnOpenPreview(fileData)
        }
        is Effect.Inner.SaveDraft -> flow {
            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            draftRepository.saveDraft(ticketId, effect.draft)
        }
        is Effect.Inner.ReadTicketIfNeed -> flow {
            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            val commands = commandsStore.getCommands(ticketId)
            val command = commands.find { it.command.commandType == TicketCommandType.MarkTicketAsRead.ordinal }
            if (localTicketsStore.getTicketWithComments(ticketId)?.ticket?.isRead == false && command == null )
                repository.readTicket(user, effect.ticketId)
        }
        is Effect.Inner.ListenAttachVariant -> flow {
            if (effect.uri !is Uri) return@flow

            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            val fileUri = runCatching { fileManager.copyFile(effect.uri) }.getOrNull()
            if (fileUri == null) {
                emit(Message.Inner.ShowToast(R.string.psd_unsupptorted_attachment.textRes()))
                return@flow
            }
            preferencesManager.saveLastActiveTime(System.currentTimeMillis())
            repository.addAttachComment(user, ticketId, fileUri)
        }
        is Effect.Inner.ListenErrorCommentAction -> flow {
            if (effect.action !is ErrorCommentAction) return@flow
            when(effect.action) {
                ErrorCommentAction.DELETE -> repository.removeCommand(effect.localId)
                ErrorCommentAction.RETRY -> repository.retryAddComment(user, effect.localId)
            }
        }
        is Effect.Inner.CancelFileUpload -> flow {
            repository.cancelUploadFile(effect.localId, effect.attachmentId)
        }

        is Effect.Inner.SubscribeToRecord -> callbackFlow {
             audioRecordController.setAudioRecordedListener { audio ->
                 Log.d("DFD", "listener onAudioRecorded")
                 trySend(Message.Inner.OnAudioRecorded(audio))
             }
             awaitClose { audioRecordController.removeRecordListener() }
        }
        is Effect.Inner.SubscribeToRecordProgress -> callbackFlow {
             audioRecordController.setProgressListener { segments ->
                 Log.d("DFD", "onRecordingProgressUpdated")
                 trySend(Message.Inner.OnRecordingProgressUpdated(segments))
             }
             awaitClose { audioRecordController.removeProgressListener() }
        }.sample(70)
        is Effect.Inner.SubscribeToCancelRecord -> callbackFlow {
             audioRecordController.setRecordCancelledListener {
                 Log.d("DFD", "listener onAudioCancelled")
                 trySend(Message.Inner.OnAudioCancelled)
             }
             awaitClose { audioRecordController.removeCancelledListener() }
        }

        is Effect.Inner.StartRecord -> flow {
            Log.d("DFD", "StartRecord")
            audioRecordController.startRecord()
        }
        is Effect.Inner.StopRecord -> flow {
            Log.d("DFD", "StopRecord")
            audioRecordController.stopRecord()
        }
        is Effect.Inner.CancelRecord -> flow {
            Log.d("DFD", "CancelRecord")
            audioRecordController.cancelRecord()
        }
        is Effect.Inner.SendAudio -> flow {
            Log.d("DFD", "SendAudio")
            ticketId = localTicketsStore.getTickets().lastOrNull()?.ticketId ?: ticketId
            idStore.setTicketId(ticketId)
            val fileUri = runCatching { File(effect.file).toUri() }.getOrNull() ?: return@flow
            preferencesManager.saveLastActiveTime(System.currentTimeMillis())
            repository.addAttachComment(user, ticketId, fileUri)
        }
        is Effect.Inner.DeleteFile -> flow {
            Log.d("DFD", "DeleteFile")
            runCatching { File(effect.file).delete() }
        }
        is Effect.Inner.PauseAudioIf -> flow {
            Log.d("DFD", "PauseAudioIf")
            withContext(Dispatchers.Main) {
                audioWrapper.pauseIf(effect.file)
            }
        }
        is Effect.Inner.PauseAudio -> flow {
            Log.d("DFD", "PauseAudio")
            withContext(Dispatchers.Main) {
                audioWrapper.pause()
            }
        }
        is Effect.Inner.UpdateAudioData -> flow {
            val ticket = localTicketsStore.getTicketWithComments(ticketId)
            val account = accountStore.getAccount()
            val user = account.getUsers().find { it.userId == ticket?.ticket?.userId }
            val uriList = ticket?.comments?.flatMap { comments ->
                comments.attachments.filter { it.name.isAudio() }.map { getFileUrl(it.id, account, user) }
            } ?: return@flow
            audioWrapper.setAudioDurations(uriList)
        }
    }

}