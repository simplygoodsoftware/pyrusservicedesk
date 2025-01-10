package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.github.terrakok.cicerone.Router
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getOrganisationLogoUrl
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val account: Account.V3,
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val router: Router,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(account, ContentState.Loading),
        reducer = FeatureReducer(),
        actor = TicketsActor(repository, router).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets(false), Effect.Inner.TicketsSetFlow
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
                val users = state.account.users

                // TODO если выбран фильтр нужно выбирать пользователя по фильтру
                val firstUser = users.first()

                if (users.size > 1) {
                    effects { +Effect.Outer.ShowAddTicketMenu(selectedAppId, users) }
                }
                else {
                    val user = UserInternal(firstUser.userId, selectedAppId)
                    effects { +Effect.Inner.OpenTicketScreen(user, null) }
                }
            }

            is Message.Outer.OnFilterClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val selectedAppId = contentState.appId ?: return
                val users = state.account.users
                effects { +Effect.Outer.ShowFilterMenu(selectedAppId, message.selectedUserId, users) }
            }

            // TODO a нам точно нужно это тут? Sd ничего не должен знать о кнопках
            Message.Outer.OnScanClick -> effects {
                +Effect.Outer.OpenQrFragment
            }

            is Message.Outer.OnSettingsClick -> effects {
                +Effect.Outer.OpenSettingsFragment
            }

            is Message.Outer.OnChangePage -> {
                val currentState = state.contentState as? ContentState.Content ?: return
                state { state.copy(contentState = updateTicketsFilterState(currentState, message.appId, account.domain)) }
            }

            Message.Outer.OnCreateTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.appId ?: return

                val users = state.account.users

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

                val user = state.account.users.find { it.userId == message.userId }
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
                        tickets = message.tickets.ticketSetInfoList,
                    )
                    ContentState.Error,
                    ContentState.Loading -> createInitialContentState(message.tickets, account.domain)
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
                state { state.copy(contentState = contentState.copy(tickets = message.tickets?.ticketSetInfoList)) }
            }
        }
    }

    private fun createInitialContentState(tickets: TicketsInfo, domain: String?) : ContentState.Content {
        val firstSet = tickets.ticketSetInfoList.firstOrNull()
        return ContentState.Content(
            appId = firstSet?.appId,
            titleText = firstSet?.orgName,
            titleImageUrl = firstSet?.orgLogoUrl?.let { getOrganisationLogoUrl(it, domain) },
            filterName = null,
            filterEnabled = false,
            tickets = tickets.ticketSetInfoList,
            filterId = null,
        )
    }

    private fun updateTicketsFilterState(state: ContentState.Content, appId: String, domain: String?) : ContentState.Content {
        val ticketsSetByAppName = state.tickets?.associateBy { it.appId }
        return state.copy(
            appId = appId,
            titleText = ticketsSetByAppName?.get(appId)?.orgName,
            titleImageUrl = ticketsSetByAppName?.get(appId)?.orgLogoUrl?.let { getOrganisationLogoUrl(it, domain) } ,
            filterName = null,
            filterEnabled = false,
            filterId = null,
        )
    }

}

internal class TicketsActor(
    private val repository: Repository,
    private val router: Router,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {

        is Effect.Inner.UpdateTickets -> singleFlow {
            when(val ticketsTry = repository.getAllData(effect.force)) {
                is Try.Success -> Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                is Try.Failure -> Message.Inner.UpdateTicketsFailed
            }
        }

        Effect.Inner.TicketsSetFlow -> repository.getAllDataFlow().map { Message.Inner.TicketsUpdated(it) }

        is Effect.Inner.OpenTicketScreen -> flow {
            // TODO readTicket перенести в TicketScreen
//            if (effect.userId != null && effect.ticketId != null)
//                repository.readTicket(
//                    user = effect.userId,
//                    ticketId = effect.ticketId
//                )
            router.navigateTo(Screens.TicketScreen(effect.ticketId, effect.user))
        }

    }

}
