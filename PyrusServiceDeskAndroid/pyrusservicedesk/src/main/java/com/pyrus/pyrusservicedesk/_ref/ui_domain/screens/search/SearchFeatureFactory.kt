package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.State
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class SearchFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: SdRepository,
    private val router: PyrusRouter,
    private val accountStore: AccountStore,
) {

    fun create(): SearchFeature = storeFactory.create(
        name = "SearchFeature",
        initialState = State(
            currentSearchText = "",
            suggestions = emptyList(),
            requestSearchText = "",
        ),
        reducer = FeatureReducer(),
        actor = FeatureActor(repository, router, accountStore).adaptCast(),
    ).adapt { it as? Effect.Out }

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            Message.Out.OnCloseClick -> effects { +Effect.Out.Exit }
            is Message.Out.OnSearchChanged -> {
                val query = message.text.trim()
                if (query.isBlank()) {
                    state { state.copy(
                        currentSearchText = "",
                        suggestions = emptyList(),
                        requestSearchText = ""
                    ) }
                }
                else {
                    state { state.copy(currentSearchText = query) }
                    effects { +Effect.In.SearchText(text = query) }
                }
            }
            is Message.Out.OnTicketClick -> effects {
                +Effect.In.OpenTicket(
                    ticketId = message.ticketId,
                    commentId = message.commentId,
                    userId = message.userId
                )
                +Effect.Out.CloseKeyBoard
            }
            is Message.In.SearchSuccess -> {
                if (message.query != state.currentSearchText) {
                    return
                }
                state { state.copy(
                    requestSearchText = message.query,
                    suggestions = message.suggestions
                ) }
            }
            is Message.In.Exit -> effects { +Effect.Out.Exit }
            is Message.In.OpenTicket -> effects { +Effect.Out.OpenTicket(message.ticketId, message.commentId, message.user) }
        }
    }

}

private class FeatureActor(
    private val repository: SdRepository,
    private val router: PyrusRouter,
    private val accountStore: AccountStore,
): Actor<Effect.In, Message.In> {

    override fun handleEffect(effect: Effect.In): Flow<Message.In> = when(effect) {
        is Effect.In.SearchText -> flow {
            val suggestions = repository.searchTickets(effect.text, 50)
            emit(Message.In.SearchSuccess(suggestions, effect.text))
        }

        Effect.In.CloseScreen -> flow { router.exit() }
        is Effect.In.OpenTicket -> flow {
            accountStore.getAccount().getUsers().find { it.userId == effect.userId }?.let {
                val user = UserInternal(it.userId, it.appId)
                emit(Message.In.OpenTicket(effect.ticketId, effect.commentId, user))
            }

        }
    }

}