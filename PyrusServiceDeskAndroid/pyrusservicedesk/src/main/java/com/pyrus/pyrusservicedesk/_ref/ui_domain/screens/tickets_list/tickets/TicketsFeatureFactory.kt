package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import androidx.core.os.bundleOf
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListFragment.Companion.KEY_USER_ID
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.sdk.data.User
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
            tickets = Tickets(
                null,
                commandsResult = emptyList()
            ),
            isLoading = true,
            titleText = "",
            titleImageUrl = "",
            filterName = "",
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutVisibility = false,
        ),
        reducer = FeatureReducer(),
        actor = TicketsActor(syncRepository).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets,
        ),
    )

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
        //state { state.copy() }
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
//                else if (users.isEmpty()){
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

            //is Message.Outer.OnScanClick -> TODO()

            //is Message.Outer.OnSettingsClick -> TODO()

            is Message.Outer.OnChangeApp -> state {
                updateTicketsState(state, message.appId)
            }
            else -> { PLog.d(TAG, "OnScanClick, OnSettingsClick")}
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            //Message.Inner.UpdateTicketsFailed -> TODO()
            is Message.Inner.UpdateTicketsCompleted -> {
                //state.copy(isLoading = false)
                state { setTicketsState(message.tickets) }
                //setTicketsState(message.tickets)
                //Message.Inner.TicketsUpdated(message.tickets)
            }
            is Message.Inner.TicketsUpdated -> {
                setTicketsState(message.tickets)
            }
            is Message.Inner.UserIdSelected -> {
                state {
                    state.copy(
                        filterName = injector().usersAccount?.users?.find { it.userId == message.userId }?.userName
                            ?: "",
                        filterEnabled = message.userId != KEY_DEFAULT_USER_ID,
                    )
                }
                message.fm.setFragmentResult(KEY_USER_ID, bundleOf(KEY_USER_ID to message.userId))
            }

            else -> {PLog.d(TAG, "UpdateTicketsFailed")}
        }
    }

    private fun getSelectedUsers(appId: String?): List<User>? {
        if (appId == null)
            return emptyList()

        return injector().usersAccount?.users?.filter { it.appId == appId }
    }

    private fun setTicketsState(tickets: Tickets) : State {
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
            //repository.startSync()
            val ticketsTry: Try<Tickets> = repository.getAllData()
            when {
                ticketsTry.isSuccess() -> Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                else -> Message.Inner.UpdateTicketsFailed
            }

        }
        Effect.Inner.TicketsAutoUpdate -> flow {}
    }

}
