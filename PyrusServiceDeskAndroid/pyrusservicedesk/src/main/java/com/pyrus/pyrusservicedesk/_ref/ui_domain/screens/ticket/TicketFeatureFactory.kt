package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.RatingEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.WelcomeMessageEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouterImpl
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.plus
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date

internal class TicketFeatureFactory(
    private val storeFactory: StoreFactory2,
    private val repository: Repository,
    private val draftRepository: DraftRepository,
    private val welcomeMessage: String,
    private val router: PyrusRouter,
) {

    fun create(): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State(
            comments = null,
            isLoading = true,
            sendEnabled = false,
            inputText = draftRepository.getDraft(),
            showError = false,
            welcomeMessage = welcomeMessage
        ),
        reducer = FeatureReducer(),
        actor = TicketActor(repository, router).adaptCast(),
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
        state { state.copy(sendEnabled = state.inputText.isNotBlank()) }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            is Message.Outer.OnAttachmentSelected -> TODO("send comment with attachment")
            Message.Outer.OnCloseClick -> effects { +Effect.Inner.Close }
            is Message.Outer.OnCopyClick -> effects { +Effect.Inner.CopyToClipboard(message.text) }
            is Message.Outer.OnMessageChanged -> state { state.copy(inputText = message.text) }
            is Message.Outer.OnPreviewClick -> effects { +Effect.Inner.OpenPreview(message.uri) }
            is Message.Outer.OnRatingClick -> {

            }
            is Message.Outer.OnRetryClick -> {
                val comment = state.comments?.comments?.find {
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
                val comment = state.inputText
                if (comment.isBlank()) return
                effects { +Effect.Inner.SendComment(comment) }
            }
            Message.Outer.OnShowAttachVariantsClick -> TODO("open attach variants dialog screen")
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateCommentsFailed -> state { state.copy(isLoading = false) }
            Message.Inner.UpdateCommentsCompleted -> state { state.copy(isLoading = false) }
            is Message.Inner.CommentsUpdated -> state { state.copy(comments = message.comments) }
        }
    }

}

internal class TicketActor(
    private val repository: Repository,
    private val router: PyrusRouter
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try<Comments> = repository.getFeed(keepUnread = false)
            when {
                commentsTry.isSuccess() -> Message.Inner.UpdateCommentsCompleted
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