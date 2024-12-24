package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import android.net.Uri
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getFileUrl
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
import com.pyrus.pyrusservicedesk.sdk.data.CommandDto
import com.pyrus.pyrusservicedesk.sdk.data.CreateCommentDto
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

internal class TicketFeatureFactory(
    private val account: Account,
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val draftRepository: DraftRepository,
    private val router: PyrusRouter,
    private val fileManager: FileManager,
) {

    fun create(
        userId: String,
        ticketId: Int,
        welcomeMessage: String,
    ): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State.Loading,
        reducer = FeatureReducer(),
        actor = TicketActor(ticketId, account, repository, router, welcomeMessage, draftRepository, fileManager).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.FeedFlow,
            Effect.Inner.CommentsAutoUpdate,
            Effect.Inner.UpdateComments(ticketId, userId),
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
                effects { +Effect.Inner.SendAttachComment(
                    message.fileUri,
                    ticketId = (state as State.Content).ticketId,
                    appId = (state as State.Content).appId,
                    userId = (state as State.Content).userId
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
                    appId = (state as State.Content).appId,
                    userId = (state as State.Content).userId
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
                effects { +Effect.Inner.SendTextComment(comment, currentState.ticketId, currentState.appId, currentState.userId) }
            }
            is Message.Outer.OnShowAttachVariantsClick -> {
                if(state !is State.Content) return
                effects { +Effect.Outer.ShowAttachVariants }
            }
            is Message.Outer.OnRefresh -> {
                val currentState = state as? State.Content ?: return
                if (currentState.isLoading) return
                state { currentState.copy(isLoading = true) }
                effects { +Effect.Inner.UpdateComments(currentState.ticketId, currentState.userId) }
            }

            Message.Outer.OnBackClick -> injector().router.exit()
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateCommentsFailed -> {
                when (val currentState = state) {
                    is State.Content -> {
                        state { currentState.copy(isLoading = false) }
                        // TODO show toast
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
                        appId = injector().usersAccount?.users?.find { it.userId == message.ticket.userId }?.appId ?: "",
                        userId = message.ticket.userId ?: "",
                        ticketId = message.ticket.ticketId ?: 0
                    ) }
                    State.Error,
                    State.Loading -> state { State.Content(
                        ticket = message.ticket,
                        sendEnabled = true,
                        inputText = message.draft,
                        welcomeMessage = message.welcomeMessage,
                        isLoading = false,
                        appId = injector().usersAccount?.users?.find { it.userId == message.ticket.userId }?.appId ?: "",
                        userId = message.ticket.userId ?: "",
                        ticketId = message.ticket.ticketId ?: 0

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
    private val ticketId: Int,
    private val account: Account,
    private val repository: Repository,
    private val router: PyrusRouter,
    private val welcomeMessage: String?,
    private val draftRepository: DraftRepository,
    private val fileManager: FileManager,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when (effect) {
        is Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try<FullTicket> = repository.getFeed(
                keepUnread = false,
                includePendingComments = true,
                ticketId = effect.ticketId
            )
            when {
                commentsTry.isSuccess() -> {
                    if (commentsTry.value.ticketId == null) {
                        Message.Inner.UpdateCommentsCompleted(
                            ticket = FullTicket(
                                comments = emptyList(),
                                showRating = false,
                                showRatingText = null,
                                userId = effect.userId,
                                ticketId = effect.ticketId,
                                subject = null,
                                isRead = true,
                                lastComment = null,
                            ),
                            draft = draftRepository.getDraft(),
                            welcomeMessage = welcomeMessage,
                        )
                    } else {
                        Message.Inner.UpdateCommentsCompleted(
                            ticket = commentsTry.value,
                            draft = draftRepository.getDraft(),
                            welcomeMessage = welcomeMessage,
                        )
                    }
                }

                else -> Message.Inner.UpdateCommentsFailed
            }
        }
        Effect.Inner.FeedFlow -> repository.getFeedFlow(ticketId).map { Message.Inner.CommentsUpdated(it) }
        Effect.Inner.CommentsAutoUpdate -> flow {
//            // TODO
//            repository.getFeed(keepUnread = false)

        }
        Effect.Inner.Close -> flow { router.exit() }
        is Effect.Inner.SendTextComment -> flow { repository.addTextComment(ticketId, effect.text, getSendTextCommentCommand(effect.text, effect.appId, effect.userId, effect.ticketId)) }
        is Effect.Inner.SendRatingComment -> flow { repository.addRatingComment(ticketId, effect.rating) }
        is Effect.Inner.SendAttachComment -> flow {
            val fileUri = try { fileManager.copyFile(effect.uri) } catch (e: Exception) { null } ?: return@flow
            repository.addAttachComment(ticketId, fileUri)
        }
        is Effect.Inner.RetryAddComment -> flow { repository.retryAddComment(ticketId, effect.id) }
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
    }

    private fun getUUID(): String {
        val uuid: UUID = UUID.randomUUID()
        val commentId = repository.createLocalCommentId()
        return "commentId=$commentId;${uuid}"
    }

    private fun getSendTextCommentCommand(text: String, appId: String, userId: String, ticketId: Int): CommandDto {
        val localCreateComment = CreateCommentDto(
            comment = text,
            requestNewTicket = ticketId < 0,
            userId = userId,
            appId = appId,
            ticketId = ticketId,
            attachments = emptyList(),
        )
//        val localTicketIsRead = MarkTicketAsRead(
//            ticketId = ticketId.toString(),//TODO check why String
//            userId = userId,
//            appId = appId
//        )
//        val list = listOf(
//            Command(getUUID(), TicketCommandType.CreateComment, appId, userId,  localCreateComment),
//            Command(getUUID(), TicketCommandType.MarkTicketAsRead, appId, userId,  localTicketIsRead))
        return CommandDto(getUUID(), TicketCommandType.CreateComment, appId, userId,  localCreateComment)
    }

}