package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.MAX_FILE_SIZE_BYTES
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getFileUrl
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class TicketFeatureFactory(
    private val account: Account,
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val draftRepository: DraftRepository,
    private val router: PyrusRouter,
    private val fileManager: FileManager,
) {

    fun create(
        user: UserInternal,
        ticketId: Int,
        welcomeMessage: String?,
    ): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State.Loading,
        reducer = FeatureReducer(),
        actor = TicketActor(
            account = account,
            ticketId = ticketId,
            user = user,
            repository = repository,
            router = router,
            welcomeMessage = welcomeMessage,
            draftRepository = draftRepository,
            fileManager = fileManager
        ).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.FeedFlow,
            Effect.Inner.UpdateComments(force = false, ticketId = ticketId),
            Effect.Inner.ReadTicket(user = user, ticketId = ticketId),
        ),
    ).adapt { it as? Effect.Outer }

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
        val currentState = state
        if (currentState is State.Content) {
            state { currentState.copy(sendEnabled = currentState.inputText.isNotBlank()) }
        }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            is Message.Outer.OnAttachmentSelected -> {
                if (state !is State.Content) return
                message.fileUri ?: return // TODO Show toast
                if (message.fileSize == null || message.fileSize > MAX_FILE_SIZE_BYTES) {
                    effects {
                        +Effect.Outer.MakeToast(
                            TextProvider.Format(
                                R.string.psd_file_size_exceeded_message, listOf(
                                    MAX_FILE_SIZE_BYTES.toString()
                                )
                            )
                        )
                    }
                    return
                }
                effects { +Effect.Inner.SendAttachComment(
                    message.fileUri,
                    ticketId = (state as State.Content).ticketId,
                ) }
            }
            Message.Outer.OnCloseClick -> effects { +Effect.Inner.Close }
            is Message.Outer.OnCopyClick -> effects {
                +Effect.Outer.CopyToClipboard(message.text)
                +Effect.Outer.MakeToast(R.string.psd_copied_to_clipboard.textRes())
            }
            is Message.Outer.OnMessageChanged -> {
                val currentState = state as? State.Content ?: return
                state { currentState.copy(inputText = message.text) }
                effects { +Effect.Inner.SaveDraft(message.text) }
            }
            is Message.Outer.OnPreviewClick -> {
                val currentState = state as? State.Content ?: return
                val attach = currentState.ticket?.comments
                    ?.find { it.id == message.commentId }?.attachments
                    ?.find { it.id == message.attachmentId } ?: return

                effects { +Effect.Inner.OpenPreview(attach) }
            }
            is Message.Outer.OnRatingClick -> {
                if (state !is State.Content) return
                effects { +Effect.Inner.SendRatingComment(
                    message.rating,
                    ticketId = (state as State.Content).ticketId,
                ) }
            }
            is Message.Outer.OnRetryAddCommentClick -> {
                if (state !is State.Content) return
                effects { +Effect.Inner.RetryAddComment(message.id) }
            }
            is Message.Outer.OnSendClick -> {
                val currentState = state as? State.Content ?: return
                val comment = currentState.inputText
                if (comment.isBlank()) return
                effects { +Effect.Inner.SendTextComment(comment, currentState.ticketId) }
            }
            is Message.Outer.OnShowAttachVariantsClick -> {
                if(state !is State.Content) return
                effects { +Effect.Outer.ShowAttachVariants }
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

            Message.Outer.OnBackClick -> effects { +Effect.Inner.Close }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            is Message.Inner.UpdateCommentsFailed -> {
                when (val currentState = state) {
                    is State.Content -> {
//                        if (message.error == Exception("data not found")) {
//                            effects { +Effect.Inner.Close }
//                        } //TODO надо как-то обрабатывать разные ошибки Try2?
                        state { currentState.copy(isLoading = false) }
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
                        ticketId = message.ticket.ticketId
                    ) }
                    State.Error,
                    State.Loading -> state { State.Content(
                        ticket = message.ticket,
                        sendEnabled = true,
                        inputText = message.draft,
                        welcomeMessage = message.welcomeMessage,
                        isLoading = false,
                        ticketId = message.ticket.ticketId

                    ) }
                }
            }
            is Message.Inner.CommentsUpdated -> {
                val currentState = state as? State.Content ?: return
                state { currentState.copy(ticket = message.ticket) }
            }
        }
    }

}

internal class TicketActor(
    private val account: Account,
    private val ticketId: Int,
    private val user: UserInternal,
    private val repository: Repository,
    private val router: PyrusRouter,
    private val welcomeMessage: String?,
    private val draftRepository: DraftRepository,
    private val fileManager: FileManager,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when (effect) {
        is Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try<FullTicket> = repository.getFeed(
                ticketId = effect.ticketId,
                userId = user.userId,
                force = effect.force
            )
            when {
                commentsTry.isSuccess() -> {
                    Message.Inner.UpdateCommentsCompleted(
                        ticket = commentsTry.value,
                        draft = draftRepository.getDraft(),
                        welcomeMessage = welcomeMessage,
                    )
                }
                else -> Message.Inner.UpdateCommentsFailed
            }
        }
        Effect.Inner.FeedFlow -> repository.getFeedFlow(ticketId).map { Message.Inner.CommentsUpdated(it) }
        Effect.Inner.Close -> flow { router.exit() }
        is Effect.Inner.SendTextComment -> flow {
            repository.addTextComment(user, ticketId, effect.text)
        }
        is Effect.Inner.SendRatingComment -> flow {
            repository.addRatingComment(user, ticketId, effect.rating)
        }
        is Effect.Inner.SendAttachComment -> flow {
            val fileUri = try { fileManager.copyFile(effect.uri) } catch (e: Exception) { null } ?: return@flow
            repository.addAttachComment(user, ticketId, fileUri)
        }
        is Effect.Inner.RetryAddComment -> flow { repository.retryAddComment(user, ticketId, effect.id) }
        is Effect.Inner.OpenPreview -> flow {
            val fileDate = FileData(
                effect.attachment.name,
                effect.attachment.bytesSize,
                Uri.parse(getFileUrl(effect.attachment.id, account)),
                false,
            )
            router.navigateTo(Screens.ImageScreen(fileDate))
        }
        is Effect.Inner.SaveDraft -> flow { draftRepository.saveDraft(effect.draft) }
        is Effect.Inner.ReadTicket -> flow { repository.readTicket(effect.user, effect.ticketId) }
    }

}