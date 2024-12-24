package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.util.Log
import androidx.core.os.bundleOf
import com.github.terrakok.cicerone.Router
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_USER_ID
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory,
    private val syncRepository: Repository,
    private val router: Router,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State.Loading,
        reducer = FeatureReducer(),
        actor = TicketsActor(syncRepository, router).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets, Effect.Inner.TicketsSetFlow
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
                val currentState = state as? State.Content ?: return
                val selectedAppId = currentState.appId ?: return
                val users = getSelectedUsers(currentState.appId) ?: emptyList()

                if (users.size > 1) {
                    effects { +Effect.Outer.ShowAddTicketMenu(selectedAppId) }
                }
                else if (users.isEmpty()) {
                    effects { +Effect.Inner.OpenTicketScreen(null, null) }
                }
                else {
                    // TODO looks strange
                    effects { +Effect.Inner.OpenTicketScreen(null, users.first().userId) }
                }
            }

            is Message.Outer.OnFilterClick -> {
                val currentState = state as? State.Content ?: return
                val selectedAppId = currentState.appId ?: return
                effects { +Effect.Outer.ShowFilterMenu(selectedAppId, message.selectedUserId) }
            }

            // TODO
            Message.Outer.OnScanClick -> injector().router.exit()

            // TODO
            Message.Outer.OnSettingsClick -> Log.d(TAG, "OnScanClick, OnSettingsClick")

            is Message.Outer.OnChangePage -> {
                val currentState = state as? State.Content ?: return
                state { updateTicketsFilterState(currentState, message.appId) }
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
            is Message.Outer.OnTicketClick -> {
                effects { +Effect.Inner.OpenTicketScreen(message.ticketId, message.userId) }
            }
            is Message.Outer.OnUserIdSelect -> TODO("why")
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateTicketsFailed -> {
                val currentState = state
                when(currentState) {
                    is State.Content -> {}
                    State.Error -> {}
                    State.Loading -> state { State.Error }
                }
            }
            is Message.Inner.UpdateTicketsCompleted -> {
                state { setTicketsState(message.tickets) }
            }
            is Message.Inner.TicketsUpdated -> {
                state { setTicketsState(message.tickets) }
            }
            is Message.Inner.UserIdSelected -> {
                val currentState = state as? State.Content ?: return

                state {
                    currentState.copy(
                        // TODO AccountRepository
                        // TODO wtf
                        filterName = injector().usersAccount?.users?.find { it.userId == message.userId }?.userName
                            ?: "",
                        filterEnabled = message.userId != KEY_DEFAULT_USER_ID,
                    )
                }

                // TODO WTF!!!!
                message.fm.setFragmentResult(KEY_USER_ID, bundleOf(KEY_USER_ID to message.userId))
            }
        }
    }

    private fun getSelectedUsers(appId: String?): List<User>? {
        return if (appId == null) emptyList()
        else injector().usersAccount?.users?.filter { it.appId == appId } // TODO wtf
    }

    private fun setTicketsState(tickets: TicketsInfo) : State {

        return State.Content(
            appId = tickets.ticketSetInfoList.firstOrNull()?.appId,
            titleText = tickets.ticketSetInfoList.firstOrNull()?.orgName,
            titleImageUrl = tickets.ticketSetInfoList.firstOrNull()?.orgLogoUrl,
            filterName = null,
            filterEnabled = false,
            tickets = tickets.ticketSetInfoList,
        )
    }

    private fun updateTicketsFilterState(state: State.Content, appId: String) : State {
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
    private val router: Router,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {

        Effect.Inner.UpdateTickets -> singleFlow {
            val ticketsTry: Try<TicketsInfo> = repository.getAllData()
            when {
                ticketsTry.isSuccess() -> Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                else -> Message.Inner.UpdateTicketsFailed
            }
        }

        is Effect.Inner.OpenTicketScreen -> flow {
            router.navigateTo(Screens.TicketScreen(effect.ticketId, effect.userId))
        }

        Effect.Inner.TicketsSetFlow -> flow {

            repository // TODO
        }
    }

}
