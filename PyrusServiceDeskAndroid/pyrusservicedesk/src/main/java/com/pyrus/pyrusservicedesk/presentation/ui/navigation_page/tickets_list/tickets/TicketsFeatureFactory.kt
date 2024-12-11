package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import androidx.core.os.bundleOf
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListFragment.Companion.KEY_USER_ID
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory2,
    private val syncRepository: SyncRepository
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(
            appId = "",
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
        ),
        reducer = FeatureReducer(),
        actor = TicketsActor(syncRepository).adaptCast(),
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
            is Message.Outer.OnFabItemClick -> {
                if (message.users.size > 1) {
                    Effect.Outer.ShowAddTicketMenu(state.appId)
                }
                else if (message.users.isEmpty()){
                    Effect.Outer.ShowTicket()
                }
                else {
                    Effect.Outer.ShowTicket(userId = message.users[message.users.keys.first()])
                }
            }

            is Message.Outer.OnFilterClick -> {
                Effect.Outer.ShowFilterMenu(state.appId, message.selectedUserId)
            }

            is Message.Outer.OnScanClick -> TODO()

            is Message.Outer.OnSettingsClick -> TODO()

        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateTicketsFailed -> TODO()
            Message.Inner.UpdateTicketsCompleted -> TODO()
            is Message.Inner.TicketsUpdated -> TODO()
            is Message.Inner.UserIdSelected -> {
                state { State(
                    appId = PyrusServiceDesk.users.find { it.userId == message.userId }?.appId ?: "",
                    titleText = state.titleText,
                    titleImageUrl = state.titleImageUrl,
                    filterName = PyrusServiceDesk.users.find { it.userId == message.userId }?.userName
                        ?: "",
                    ticketsIsEmpty = state.ticketsIsEmpty,
                    filterEnabled = message.userId != KEY_DEFAULT_USER_ID,
                    tickets = state.tickets,
                    isLoading = state.isLoading
                ) }
                message.fm.setFragmentResult(KEY_USER_ID, bundleOf(KEY_USER_ID to message.userId))
            }
        }
    }

}

internal class TicketsActor(
    private val repository: SyncRepository,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        Effect.Inner.UpdateTickets -> TODO()/*singleFlow {
            val ticketsTry: Try<Comments> = repository.startSync() getFeed(
                keepUnread = false,
                requestsRemoteComments = true
            )
            when {
                commentsTry.isSuccess() -> Message.Inner.UpdateCommentsCompleted
                else -> Message.Inner.UpdateCommentsFailed
            }

        }*/
        Effect.Inner.TicketsAutoUpdate -> flow {
            // TODO

        }
    }

}
