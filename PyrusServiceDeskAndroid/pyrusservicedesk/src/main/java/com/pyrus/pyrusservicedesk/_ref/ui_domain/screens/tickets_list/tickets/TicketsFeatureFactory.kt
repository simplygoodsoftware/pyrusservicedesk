package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.github.terrakok.cicerone.Router
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getOrganisationLogoUrl
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
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
    private val accountStore: AccountStore,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(ContentState.Loading),
        reducer = FeatureReducer(),
        actor = TicketsActor(repository, router, commandsStore, accountStore).adaptCast(),
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
            Message.Outer.OnFabItemClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val selectedAppId = contentState.appId ?: return
                val users = contentState.account.getUsers().filter { it.appId == contentState.appId }

                val selectedUser = contentState.filterId?.let { id -> users.find { it.userId == id } }

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
                val selectedAppId = contentState.appId ?: return
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
                state { state.copy(contentState = contentState.copy(isLoading = true)) }
                effects { +Effect.Inner.UpdateTickets(force = true) }
            }
            is Message.Outer.OnChangePage -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val users = contentState.account.getUsers().filter { it.appId == message.appId }
                val selectedUser = users.find { it.userId == message.currentUserId }

                state { state.copy(contentState = updateTicketsFilterState(
                    state = contentState,
                    appId = message.appId,
                    domain = contentState.account.domain,
                    user = selectedUser
                )) }
            }

            Message.Outer.OnCreateTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.appId ?: return

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
                val appId = contentState.appId ?: return
                effects {
                    val user = UserInternal(message.userId, appId)
                    +Effect.Inner.OpenTicketScreen(user, message.ticketId)
                }
            }
            is Message.Outer.OnUserIdSelected -> {

                val contentState = state.contentState as? ContentState.Content ?: return

                val user = contentState.account.getUsers().find { it.userId == message.userId }
                val filterName = user?.userName?: ""
                state {
                    state.copy(contentState = contentState.copy(
                        filterName = filterName,
                        filterEnabled = message.userId != KEY_DEFAULT_USER_ID,
                        filterId = user?.userId
                    ))
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
                        isLoading = false,
                    )
                    ContentState.Error,
                    ContentState.Loading -> createInitialContentState(message.ticketsInfo)
                })
            }
            Message.Inner.UpdateTicketsFailed -> {
                when(state.contentState) {
                    is ContentState.Content -> {}
                    ContentState.Error -> {}
                    ContentState.Loading -> state { state.copy(contentState = ContentState.Error) }
                }
            }
            is Message.Inner.TicketsUpdated -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                state { state.copy(contentState = contentState.copy(
                    account = message.ticketsInfo.account,
                    ticketSets = message.ticketsInfo.ticketSetInfoList,
                    isLoading = false
                )) }
            }
            is Message.Inner.UserUpdated -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                state { state.copy(contentState = contentState.copy(
                    tabLayoutVisibility = message.tabLVisibility
                )) }
            }
        }
    }

    private fun createInitialContentState(ticketsInfo: TicketsInfo) : ContentState.Content {
        val firstSet = ticketsInfo.ticketSetInfoList.firstOrNull()
        val usersSize = (ticketsInfo.account as? Account.V3)?.users?.size ?: 0
        return ContentState.Content(
            account = ticketsInfo.account,
            appId = firstSet?.appId,
            titleText = firstSet?.orgName,
            titleImageUrl = firstSet?.orgLogoUrl?.let { getOrganisationLogoUrl(it, ticketsInfo.account.domain) },
            filterName = null,
            filterEnabled = false,
            ticketSets = ticketsInfo.ticketSetInfoList,
            filterId = null,
            isLoading = false,
            tabLayoutVisibility = usersSize > 1
        )
    }

    private fun updateTicketsFilterState(
        state: ContentState.Content,
        appId: String,
        domain: String?,
        user: User?,
    ) : ContentState.Content {
        val ticketsSetByAppName = state.ticketSets?.associateBy { it.appId }
        return state.copy(
            appId = appId,
            titleText = ticketsSetByAppName?.get(appId)?.orgName,
            titleImageUrl = ticketsSetByAppName?.get(appId)?.orgLogoUrl?.let { getOrganisationLogoUrl(it, domain) } ,
            filterName = user?.userName,
            filterEnabled = user != null && user.userId != KEY_DEFAULT_USER_ID,
            filterId = user?.userId,
        )
    }

}

internal class TicketsActor(
    private val repository: Repository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
    private val accountStore: AccountStore,
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

        Effect.Inner.TicketsSetFlow -> repository.getAllDataFlow()
            .debounce(150)
            .map { Message.Inner.TicketsUpdated(it) }

        is Effect.Inner.OpenTicketScreen -> flow {
            val ticketId = effect.ticketId ?: commandsStore.getNextLocalId()
            router.navigateTo(Screens.TicketScreen(ticketId, effect.user).setSlideRightAnimation())
        }

        is Effect.Inner.CheckAccount -> flow {
            accountStore.accountStateFlow().collect { account ->
                val users = account.getUsers()
                Message.Inner.UserUpdated(users.size > 1)
            }
        }

    }

}
