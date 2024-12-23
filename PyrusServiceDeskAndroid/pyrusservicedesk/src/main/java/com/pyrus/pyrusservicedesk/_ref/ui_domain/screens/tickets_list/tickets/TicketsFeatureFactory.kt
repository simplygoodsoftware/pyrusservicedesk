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
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
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
            tickets = null,
            isLoading = true,
            titleText = "",
            titleImageUrl = "",
            filterName = "",
            filterEnabled = false,
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
                    val selectedAppId = state.appId ?: return
                    effects { +Effect.Outer.ShowAddTicketMenu(selectedAppId) }
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

            is Message.Outer.OnFilterClick -> {
                val selectedAppId = state.appId ?: return
                effects { +Effect.Outer.ShowFilterMenu(selectedAppId, message.selectedUserId) }
            }

            Message.Outer.OnScanClick -> injector().router.exit() // TODO

            Message.Outer.OnSettingsClick -> Log.d(TAG, "OnScanClick, OnSettingsClick")

            is Message.Outer.OnChangeApp -> state {
                updateTicketsFilterState(state, message.appId)
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

    private fun setTicketsState(tickets: TicketsInfo) : State {

        return State(
            appId = tickets.ticketSetInfoList.firstOrNull()?.appId,
            titleText = tickets.ticketSetInfoList.firstOrNull()?.orgName,
            titleImageUrl = tickets.ticketSetInfoList.firstOrNull()?.orgLogoUrl,
            filterName = null,
            filterEnabled = false,
            tickets = tickets.ticketSetInfoList,
            isLoading = false,
            showNoConnectionError = false,
        )
    }

    private fun updateTicketsFilterState(state: State, appId: String) : State {
        val ticketsSetByAppName = state.tickets?.associateBy { it.appId }
        return state.copy(
            appId = appId,
            titleText = ticketsSetByAppName?.get(appId)?.orgName,
            titleImageUrl = ticketsSetByAppName?.get(appId)?.orgLogoUrl,
            filterName = null,
            filterEnabled = false,
        )
    }

}

internal class TicketsActor(
    private val repository: Repository,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {

        Effect.Inner.UpdateTickets -> singleFlow {
            val ticketsTry: Try<TicketsInfo> = repository.getAllData()
            when {
                ticketsTry.isSuccess() -> Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                else -> Message.Inner.UpdateTicketsFailed
            }
        }
    }

}
