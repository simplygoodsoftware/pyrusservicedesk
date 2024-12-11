package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.State
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "TicketsListFeature"

internal class TicketsListFeatureFactory(
    private val storeFactory: StoreFactory2,
    private val syncRepository: SyncRepository,
    private val appId: String,
) {

    fun create(): TicketsListFeature = storeFactory.create(
        name = TAG,
        initialState = State(
            allTickets = emptyList(),
            tickets = emptyList(),
            appId = appId,
            users = hashMapOf(),
            selectedUser = "",
            isLoading = true
        ),
        reducer = FeatureReducer(),
        actor = TicketsListActor(syncRepository).adaptCast(),
    )

}

private class FeatureReducer : Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when (message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
        //state { state.copy() }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {

            is Message.Outer.OnTicketClick -> {
                Effect.Outer.ShowTicket(
                    message.ticketId,
                    getUserId(message.ticketId, state.tickets)
                )
            }

            Message.Outer.OnCreateTicketClick -> {
                val users = getSelectedUsers(state.appId)
                if (users.size > 1) {
                    Effect.Outer.ShowAddTicketMenu(state.appId)
                } else {
                    Effect.Outer.ShowTicket(userId = users[users.keys.first()])
                }
            }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateTicketsFailed -> TODO()
            Message.Inner.UpdateTicketsCompleted -> TODO()
            is Message.Inner.TicketsUpdated -> TODO()
            is Message.Inner.UserIdSelected -> {
                val tickets = if (message.userId.isNotEmpty()) {
                    state.allTickets.filter { it.userId == message.userId }
                } else {
                    state.allTickets
                }
                state {
                    State(
                        allTickets = state.allTickets,
                        tickets = tickets,
                        appId = state.appId,
                        users = state.users,
                        selectedUser = message.userId,
                        isLoading = state.isLoading
                    )
                }

            }
        }
    }

    private fun getSelectedUsers(appId: String): HashMap<String, String> {
        val hashMap = hashMapOf<String, String>()
        for (user in PyrusServiceDesk.users) {
            if (user.appId == appId) {
                hashMap[user.userId] = user.userName
            }
        }
        return hashMap
    }

    private fun getUserId(ticketId: Int, tickets: List<Ticket>): String {
        return tickets.find { it.ticketId == ticketId }?.userId ?: ""
    }

}

internal class TicketsListActor(
    private val repository: SyncRepository,
) : Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when (effect) {
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
