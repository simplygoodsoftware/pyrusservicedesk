package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.github.terrakok.cicerone.Router
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(ContentState.Loading),
        reducer = FeatureReducer(),
        actor = TicketsActor(repository, router, commandsStore).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets(false),
            Effect.Inner.TicketsSetFlow,
        ),
    ).adapt { it as? Effect.Outer }

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            is Message.Outer.OnFabItemClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val selectedAppId = contentState.pageAppId ?: return
                val users = contentState.account.getUsers().filter { it.appId == contentState.pageAppId }

                val selectedUser = contentState.filter

                if (users.size > 1 && selectedUser == null) {
                    effects { +Effect.Outer.ShowAddTicketMenu(selectedAppId, users) }
                }
                else if (users.size > 1 && selectedUser != null) {
                    val user = UserInternal(selectedUser.userId, selectedAppId)
                    effects { +Effect.Inner.OpenTicketScreen(user, null) }
                }
                else {
                    val firstUser = users.firstOrNull() ?: return
                    val user = UserInternal(firstUser.userId, selectedAppId)
                    effects { +Effect.Inner.OpenTicketScreen(user, null) }
                }
            }
            is Message.Outer.OnFilterClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val selectedAppId = contentState.pageAppId ?: return
                val users = contentState.account.getUsers()
                effects { +Effect.Outer.ShowFilterMenu(selectedAppId, message.selectedUserId, users) }
            }
            is Message.Outer.OnRetryClick -> {
                if (state.contentState !is ContentState.Error) return
                state { state.copy(contentState = ContentState.Loading) }
                effects { +Effect.Inner.UpdateTickets(force = true) }
            }
            is Message.Outer.OnRefresh -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                state { state.copy(contentState = contentState.copy(isUserTriggerLoading = true)) }
                effects { +Effect.Inner.UpdateTickets(force = true) }
            }
            is Message.Outer.OnChangePage -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val currentFilter = contentState.filter
                val filter = if (currentFilter?.appId != message.appId) null else currentFilter

                state {
                    state.copy(contentState = contentState.copy(
                        pageAppId = message.appId,
                        filter = filter,
                    ))
                }
            }
            is Message.Outer.OnCreateTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.pageAppId ?: return

                val users = contentState.account.getUsers()

                val firstUser = users.first()

                if (users.size > 1) effects {
                    +Effect.Outer.ShowAddTicketMenu(appId, users)
                }
                else effects {
                    +Effect.Inner.OpenTicketScreen(
                        user = UserInternal(firstUser.userId, firstUser.appId),
                        ticketId = null,
                    )
                }
            }
            is Message.Outer.OnTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.pageAppId ?: return
                effects {
                    val user = UserInternal(message.userId, appId)
                    +Effect.Inner.OpenTicketScreen(user, message.ticketId)
                }
            }
            is Message.Outer.OnFilterSelected -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val user = contentState.account.getUsers().find { it.userId == message.userId }
                state {
                    state.copy(contentState = contentState.copy(filter = user))
                }
            }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            is Message.Inner.UpdateTicketsCompleted -> state {
                state.copy(contentState = when(val currentDateState = state.contentState) {
                    is ContentState.Content -> currentDateState.copy(
                        ticketSets = message.ticketsInfo.ticketSetInfoList,
                        isUserTriggerLoading = false,
                        loadUserIds = emptySet()
                    )
                    is ContentState.Error,
                    is ContentState.Loading -> createInitialContentState(message.ticketsInfo)
                })
            }
            is Message.Inner.UpdateTicketsFailed -> {
                when(state.contentState) {
                    is ContentState.Content -> {}
                    is ContentState.Error -> {}
                    is ContentState.Loading -> state { state.copy(contentState = ContentState.Error) }
                }
            }
            is Message.Inner.TicketsUpdated -> {
                val contentState = state.contentState as? ContentState.Content ?: return

                val fullUsers = message.ticketsInfo.account.getUsers()
                val currentUsers = contentState.account.getUsers().map { UserInternal(it.userId, it.appId) }.toSet()
                val newUsers = fullUsers.map { UserInternal(it.userId, it.appId) }.toSet()
                val diff = newUsers.minus(currentUsers)

                val lastNewUser = diff.lastOrNull()?.let { fullUsers.find { fu -> fu.userId == it.userId} }

                val filter = when {
                    lastNewUser == null -> contentState.filter
                    newUsers.count { it.appId == lastNewUser.appId } > 1 -> lastNewUser
                    else -> null
                }

                state { state.copy(contentState = contentState.copy(
                    account = message.ticketsInfo.account,
                    ticketSets = message.ticketsInfo.ticketSetInfoList,
                    isUserTriggerLoading = false,
                    filter = filter,
                    pageAppId = lastNewUser?.appId ?: contentState.pageAppId,
                    loadUserIds = contentState.loadUserIds.toMutableSet()
                        .apply { lastNewUser?.let { add(it.userId) } }
                )) }

                if (diff.isNotEmpty()) {
                    effects { +Effect.Inner.UpdateTickets(true) }
                }
            }
        }
    }

    private fun createInitialContentState(ticketsInfo: TicketsInfo) : ContentState.Content {
        val firstSet = ticketsInfo.ticketSetInfoList.firstOrNull()
        return ContentState.Content(
            account = ticketsInfo.account,
            pageAppId = firstSet?.appId,
            filter = null,
            ticketSets = ticketsInfo.ticketSetInfoList,
            isUserTriggerLoading = false,
            loadUserIds = emptySet()
        )
    }

}

@OptIn(FlowPreview::class)
internal class TicketsActor(
    private val repository: Repository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        is Effect.Inner.UpdateTickets -> singleFlow {
            when(val ticketsTry = repository.getAllData(effect.force)) {
                is Try.Success -> {
                    Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                }
                is Try.Failure -> {
                    ticketsTry.error.printStackTrace()
                    Message.Inner.UpdateTicketsFailed
                }
            }
        }
        is Effect.Inner.TicketsSetFlow -> {
            repository.getAllDataFlow()
                .debounce(150)
                .map { Message.Inner.TicketsUpdated(it) }
        }
        is Effect.Inner.OpenTicketScreen -> flow {
            val ticketId = effect.ticketId ?: commandsStore.getNextLocalId()
            router.navigateTo(Screens.TicketScreen(ticketId, effect.user).setSlideRightAnimation())
        }
    }

}
