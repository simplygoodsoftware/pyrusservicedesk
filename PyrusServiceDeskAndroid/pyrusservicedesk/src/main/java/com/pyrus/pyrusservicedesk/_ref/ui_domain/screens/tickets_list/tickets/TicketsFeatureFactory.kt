package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.util.Log
import androidx.core.os.bundleOf
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets_list.TicketsListFragment.Companion.KEY_USER_ID
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.sdk.data.User
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory,
    private val syncRepository: Repository,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(
            appId = "",
            applications = hashSetOf(),
            tickets = TicketsDto(
                hasMore = null,
                commandsResult = emptyList()
            ),
            isLoading = true,
            titleText = "",
            titleImageUrl = "",
            filterName = "",
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutVisibility = false,
            showNoConnectionError = false,
        ),
        reducer = FeatureReducer(),
        actor = TicketsActor(syncRepository).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets,
        ),
    ).adapt { it as? Effect.Outer }

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
        // state { state.copy() }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            Message.Outer.OnFabItemClick -> {
                val users = getSelectedUsers(state.appId) ?: emptyList()
                if (users.size > 1) {
                    effects {
                        +Effect.Outer.ShowAddTicketMenu(state.appId)
                    }
                }
//                else if (users.isEmpty()) {
//                    injector().ticketFeatureFactory("welcom")
//                    effects {
//                        +Effect.Outer.ShowTicket()
//                    }
//                }
                else {
                    injector().router.navigateTo(Screens.TicketScreen(null, users.first().userId))
                }
            }

            is Message.Outer.OnFilterClick -> effects {
                +Effect.Outer.ShowFilterMenu(state.appId, message.selectedUserId)
            }

            Message.Outer.OnScanClick -> injector().router.exit() // TODO

            Message.Outer.OnSettingsClick -> Log.d(TAG, "OnScanClick, OnSettingsClick")

            is Message.Outer.OnChangeApp -> state {
                updateTicketsState(state, message.appId)
            }

            Message.Outer.OnCreateTicketClick -> {
                // TODO
//                val users = getSelectedUsers(state.appId)
//                if (users.size > 1) effects {
//                    +TicketsListContract.Effect.Outer.ShowAddTicketMenu(state.appId)
//                }
//                else effects {
//                    +TicketsListContract.Effect.Outer.ShowTicket(null, userId = users[users.keys.first()])
//                }
            }
            is Message.Outer.OnTicketClick -> effects {
                // TODO
//                +Effect.Outer.ShowTicket(
//                    message.ticketId,
//                    getUserId(message.ticketId, state.tickets)
//                )
            }
            is Message.Outer.OnUserIdSelect -> TODO()
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateTicketsFailed -> {
                state { state.copy(isLoading = false, showNoConnectionError = true) }
            }
            is Message.Inner.UpdateTicketsCompleted -> {
                state { setTicketsState(message.tickets) }
            }
            is Message.Inner.TicketsUpdated -> {
                state { setTicketsState(message.tickets) }
            }
            is Message.Inner.UserIdSelected -> {
                state {
                    state.copy(
                        // TODO AccountRepository
                        filterName = injector().usersAccount?.users?.find { it.userId == message.userId }?.userName
                            ?: "",
                        filterEnabled = message.userId != KEY_DEFAULT_USER_ID,
                    )
                }
                message.fm.setFragmentResult(KEY_USER_ID, bundleOf(KEY_USER_ID to message.userId))
            }
        }
    }

    private fun getSelectedUsers(appId: String?): List<User>? {
        return if (appId == null) emptyList()
        else injector().usersAccount?.users?.filter { it.appId == appId }
    }

    private fun setTicketsState(tickets: TicketsDto) : State {
        val applications = tickets.applications?.let { HashSet(it) }
        return State(
            appId = applications?.first()?.appId ?: "",
            applications = applications ?: hashSetOf(),
            titleText = applications?.first()?.orgName ?: "",
            titleImageUrl = applications?.first()?.orgLogoUrl ?: "",
            filterName = "",
            ticketsIsEmpty = tickets.tickets?.isEmpty() ?: true,
            filterEnabled = false,
            tabLayoutVisibility = if (applications != null) applications.size > 1 else false,
            tickets = tickets,
            isLoading = false,
            showNoConnectionError = false,
        )
    }

    private fun updateTicketsState(state: State, appId: String) : State {
        return state.copy(
            appId = appId,
            titleText = state.applications.find { it.appId == appId }?.orgName ?: "",
            titleImageUrl = state.applications.find { it.appId == appId }?.orgLogoUrl ?: "",
            filterName = "",
            filterEnabled = false,
        )
    }

}

internal class TicketsActor(
    private val repository: Repository,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {

        Effect.Inner.UpdateTickets -> singleFlow {
            val ticketsTry: Try<TicketsDto> = repository.getAllData()
            when {
                ticketsTry.isSuccess() -> Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                else -> Message.Inner.UpdateTicketsFailed
            }
        }
    }

}
