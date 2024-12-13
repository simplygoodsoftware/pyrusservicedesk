package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.util.Log
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class TicketFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val draftRepository: DraftRepository,
    private val welcomeMessage: String,
    private val router: PyrusRouter,
) {

    fun create(): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State.Loading,
        reducer = FeatureReducer(),
        actor = TicketActor(repository, router, welcomeMessage, draftRepository).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.FeedFlow,
            Effect.Inner.CommentsAutoUpdate,
            Effect.Inner.UpdateComments,
        ),
    )

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
            is Message.Outer.OnAttachmentSelected -> TODO("send comment with attachment")
            Message.Outer.OnCloseClick -> effects { +Effect.Inner.Close }
            is Message.Outer.OnCopyClick -> effects { +Effect.Inner.CopyToClipboard(message.text) }
            is Message.Outer.OnMessageChanged -> {
                val currentState = state as? State.Content ?: return
                state { currentState.copy(inputText = message.text) }
            }
            is Message.Outer.OnPreviewClick -> effects { +Effect.Inner.OpenPreview(message.uri) }
            is Message.Outer.OnRatingClick -> {
                TODO()
            }
            is Message.Outer.OnRetryClick -> {
                val currentState = state as? State.Content ?: return
                val comment = currentState.comments?.comments?.find {
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
            Message.Outer.OnSendClick -> {
                val currentState = state as? State.Content ?: return
                val comment = currentState.inputText
                if (comment.isBlank()) return
                effects { +Effect.Inner.SendComment(comment) }
            }
            Message.Outer.OnShowAttachVariantsClick -> TODO("open attach variants dialog screen")
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
                    is State.Content -> state { currentState.copy(comments = message.comments) }
                    State.Error,
                    State.Loading -> state { State.Content(
                        comments = message.comments,
                        sendEnabled = true,
                        inputText = message.draft,
                        welcomeMessage = message.welcomeMessage,
                    ) }
                }
            }
            is Message.Inner.CommentsUpdated -> {
                val currentState = state as? State.Content ?: return
                state { currentState.copy(comments = message.comments) }
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
        Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try<Comments> = repository.getFeed(
                keepUnread = false,
                includePendingComments = true
            )
            when {
                commentsTry.isSuccess() -> Message.Inner.UpdateCommentsCompleted(
                    comments = commentsTry.value,
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

        is Effect.Inner.CopyToClipboard -> TODO()
        is Effect.Inner.SendComment -> TODO()
        is Effect.Inner.OpenPreview -> TODO()
    }

}