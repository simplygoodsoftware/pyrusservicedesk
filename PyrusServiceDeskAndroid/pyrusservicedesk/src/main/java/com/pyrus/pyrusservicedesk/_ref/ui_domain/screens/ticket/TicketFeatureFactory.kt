package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class TicketFeatureFactory(
    private val storeFactory: StoreFactory2,
    private val repository: Repository,
    private val draftRepository: DraftRepository,
    private val welcomeMessage: String,
) {

    fun create(): TicketFeature = storeFactory.create(
        name = "TicketFeature",
        initialState = State(
            comments = null,
            isLoading = true,
            sendEnabled = false,
            inputText = draftRepository.getDraft(),
            showError = false,
            titleText = "", // TODO
        ),
        reducer = FeatureReducer(),
        actor = TicketActor(welcomeMessage, repository).adaptCast(),
    )

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
        state { state.copy(sendEnabled = state.inputText.isNotBlank() && !state.isLoading) }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            is Message.Outer.OnAttachmentSelected -> TODO("send comment with attachment")
            Message.Outer.OnCloseClick -> TODO("close screen")
            is Message.Outer.OnCopyClick -> TODO("copy text to copy buffer")
            is Message.Outer.OnMessageChanged -> state { state.copy(inputText = message.text) }
            Message.Outer.OnPreviewClick -> TODO("show preview screen")
            is Message.Outer.OnRatingClick -> TODO("send rating comment")
            Message.Outer.OnRetryClick -> TODO("retry send comment")
            Message.Outer.OnSendClick -> TODO("send current")
            Message.Outer.OnShowAttachVariantsClick -> TODO("open attach variants dialog screen")
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateCommentsFailed -> TODO()
            Message.Inner.UpdateCommentsCompleted -> TODO()
            is Message.Inner.CommentsUpdated -> TODO()
        }
    }

}

internal class TicketActor(
    private val welcomeMessage: String,
    private val repository: Repository,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        Effect.Inner.UpdateComments -> singleFlow {
            val commentsTry: Try<Comments> = repository.getFeed(keepUnread = false)
            when {
                commentsTry.isSuccess() -> Message.Inner.UpdateCommentsCompleted
                else -> Message.Inner.UpdateCommentsFailed
            }
        }
        Effect.Inner.FeedFlow -> flow {
            repository.getFeedFlow().map {

            }

            emit(Message.Inner.CommentsUpdated(emptyList()))
        }
        Effect.Inner.CommentsAutoUpdate -> flow {
            // TODO
            repository.getFeed(keepUnread = false)

        }
    }

}