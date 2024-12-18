package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.users
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
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
import com.pyrus.pyrusservicedesk.sdk.data.Command
import com.pyrus.pyrusservicedesk.sdk.data.CreateComment
import com.pyrus.pyrusservicedesk.sdk.data.LocalDataProvider
import com.pyrus.pyrusservicedesk.sdk.data.MarkTicketAsRead
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.TicketCommandType
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

internal class TicketFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val draftRepository: DraftRepository,
    private val welcomeMessage: String,
    private val router: PyrusRouter,
    private val userId: String,
    private val ticketId: Int,
) {

    fun create(): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State.Loading,
        reducer = FeatureReducer(),
        actor = TicketActor(repository, router, welcomeMessage, draftRepository).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.FeedFlow,
            Effect.Inner.CommentsAutoUpdate,
            Effect.Inner.UpdateComments(ticketId),
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
                val currentState = state as? State.Content ?: return
                TODO("send comment with attachment")
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
            is Message.Outer.OnPreviewClick -> effects { +Effect.Inner.OpenPreview(message.uri) }
            is Message.Outer.OnRatingClick -> {
                val currentState = state as? State.Content ?: return
                TODO()
            }
            is Message.Outer.OnRetryClick -> {
                val currentState = state as? State.Content ?: return
                val comment = currentState.ticket?.comments?.find {
                    it.commentId == message.id
                } ?: return

                val attachment = comment.attachments?.firstOrNull()
                if (attachment != null) {
                    // todo send attachment
                }
                else {
                    // todo send comment
                }
            }
            is Message.Outer.OnSendClick -> {
                val currentState = state as? State.Content ?: return
                val comment = currentState.inputText
                if (comment.isBlank()) return
                effects { +Effect.Inner.SendTextComment(comment, currentState.ticketId, currentState.appId, currentState.userId) }
            }
            Message.Outer.OnShowAttachVariantsClick -> TODO("open attach variants dialog screen")
            Message.Outer.OnBackClick -> injector().router.exit()
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateCommentsFailed -> {
                if (state !is State.Loading) return
                state { State.Error }
            }
            is Message.Inner.UpdateCommentsCompleted -> {
                when(val currentState = state) {
                    is State.Content -> state {
                        currentState.copy(
                            ticket = message.ticket,
                            appId = users.find { it.userId == message.ticket?.userId }?.appId ?: "",
                            userId = message.ticket?.userId ?: "",
                            ticketId = message.ticket?.ticketId ?: 0
                        )
                    }
                    State.Error,
                    State.Loading -> state { State.Content(
                        ticket = message.ticket,
                        sendEnabled = true,
                        inputText = message.draft,
                        welcomeMessage = message.welcomeMessage,
                        appId = users.find { it.userId == message.ticket?.userId }?.appId ?: "",
                        userId = message.ticket?.userId ?: "",
                        ticketId = message.ticket?.ticketId ?: 0
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
    private val repository: Repository,
    private val router: PyrusRouter,
    private val welcomeMessage: String?,
    private val draftRepository: DraftRepository
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        is Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try<Ticket?> = repository.getFeed(
                keepUnread = false,
                includePendingComments = true,
                ticketId = effect.ticketId
            )
            when {
                commentsTry.isSuccess() -> Message.Inner.UpdateCommentsCompleted(
                    ticket = commentsTry.value,
                    draft = draftRepository.getDraft(),
                    welcomeMessage = welcomeMessage,
                )
                else -> Message.Inner.UpdateCommentsFailed
            }
        }
        Effect.Inner.FeedFlow -> {
            repository.getFeedFlow().map { Message.Inner.CommentsUpdated(it) }
        }
        Effect.Inner.CommentsAutoUpdate -> flow {
//            // TODO
//            repository.getFeed(keepUnread = false)

        }
        Effect.Inner.Close -> flow {
            router.exit()
        }
        is Effect.Inner.SendTextComment -> flow {
            val comment =  injector().localDataProvider.createLocalComment(effect.text)
            val commandsResultTry = repository.addFeedComment(
                    commands = getSendTextCommentCommands(effect.text, effect.appId, effect.userId, effect.ticketId),
                    comment = comment,
                    uploadFileHooks = null
                )
        }
        is Effect.Inner.SendAttachComment -> TODO()
        is Effect.Inner.OpenPreview -> TODO()
        is Effect.Inner.SaveDraft -> flow {
            draftRepository.saveDraft(effect.draft)
        }
    }

    private fun getUUID(): String {
        val uuid: UUID = UUID.randomUUID()
        return uuid.toString()
    }

    fun getSendTextCommentCommands(text: String, appId: String, userId: String, ticketId: Int): List<Command> {
        val localCreateComment = CreateComment(
            comment = text,
            requestNewTicket = ticketId == 0,
            userId = userId,
            appId = appId,
            ticketId = ticketId,
            attachments = emptyList(),
        )
        val localTicketIsRead = MarkTicketAsRead(
            ticketId = ticketId.toString(),//TODO check why String
            userId = userId,
            appId = appId
        )
        val list = listOf(
            Command(getUUID(), TicketCommandType.CreateComment, appId, userId,  localCreateComment),
            Command(getUUID(), TicketCommandType.MarkTicketAsRead, appId, userId,  localTicketIsRead))
        return list
    }

}