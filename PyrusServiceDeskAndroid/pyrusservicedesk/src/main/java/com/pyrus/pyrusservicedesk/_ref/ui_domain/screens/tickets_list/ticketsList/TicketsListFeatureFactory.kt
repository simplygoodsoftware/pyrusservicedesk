package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList

import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment.Companion.KEY_DEFAULT_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.State
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "TicketsListFeature"

internal class TicketsListFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val appId: String,
) {

    fun create(): TicketsListFeature = storeFactory.create(
        name = TAG,
        initialState = State(
            allTickets = emptyList(),
            tickets = emptyList(),
            appId = appId,
            selectedUser = "",
            isLoading = true
        ),
        reducer = FeatureReducer(),
        actor = TicketsListActor(repository).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets,
        ),
    ).adapt { it as? Effect.Outer }

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
                effects {
                    +Effect.Outer.ShowTicket(
                        message.ticketId,
                        getUserId(message.ticketId, state.tickets)
                    )
                }
            }

            Message.Outer.OnCreateTicketClick -> {
                val users = getSelectedUsers(state.appId)
                if (users.size > 1) {
                    effects {
                        +Effect.Outer.ShowAddTicketMenu(state.appId)
                    }
                } else {
                    effects {
                        +Effect.Outer.ShowTicket(null, userId = users[users.keys.first()])
                    }
                }
            }

            is Message.Outer.OnUserIdSelect -> {
                val tickets = if (message.userId != KEY_DEFAULT_USER_ID) {
                    state.allTickets.filter { it.userId == message.userId }
                } else {
                    state.allTickets
                }
                state {
                    state.copy(tickets = tickets)
                }
            }

            Message.Outer.OnUpdateTickets -> effects { +Effect.Inner.UpdateTickets }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            //Message.Inner.UpdateTicketsFailed -> TODO()
            is Message.Inner.UpdateTicketsCompleted -> {

                val usersId = injector().usersAccount?.users?.filter { it.appId == state.appId }?.map { it.userId } ?: emptyList()
                val filteredTicketsAppId = message.tickets.tickets?.filter {
                    usersId.any { userId -> userId == it.userId }
                }

                val filteredTicketsData = filteredTicketsAppId?.sortedWith(TicketComparator())
                val resultTickets = filteredTicketsData?.filter { it.isActive == true }?.toMutableList()
                resultTickets?.addAll(filteredTicketsData.filter { it.isActive == false })

                state {
                    State(
                        allTickets = filteredTicketsAppId ?: emptyList(),
                        tickets = resultTickets ?: emptyList(),
                        appId = state.appId,
                        selectedUser = "",
                        isLoading = false
                    )
                }
            }
            //is Message.Inner.TicketsUpdated -> TODO()
            is Message.Inner.UserIdSelected -> {
                val tickets = if (message.userId.isNotEmpty()) {
                    state.allTickets.filter { it.userId == message.userId }
                } else {
                    state.allTickets
                }
                state.copy(tickets = tickets)

            }

            else -> {PLog.d("EP ", "message")}
        }
    }

    private fun getSelectedUsers(appId: String): HashMap<String, String> {
        val hashMap = hashMapOf<String, String>()
        val users = injector().usersAccount?.users ?: return hashMap

        for (user in users) {
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
    private val repository: Repository,
) : Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when (effect) {
        Effect.Inner.UpdateTickets -> singleFlow {
            //repository.startSync()
            val ticketsTry: Try<Tickets> = repository.getAllData()// sync(repository.createSyncRequest())
            when {
                ticketsTry.isSuccess() -> Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                else -> Message.Inner.UpdateTicketsFailed
            }

        }
        Effect.Inner.TicketsAutoUpdate -> flow {
            // TODO

        }
    }

}

private class TicketComparator : Comparator<Ticket> {

    override fun compare(o1: Ticket, o2: Ticket): Int {
        return when {
            o1.lastComment == null -> return when {
                o2.lastComment == null -> o1.ticketId - o2.ticketId
                else -> 1
            }
            o2.lastComment == null -> -1
            o1.lastComment.creationDate.before(o2.lastComment.creationDate) -> 1
            o1.lastComment.creationDate.after(o2.lastComment.creationDate) -> -1
            else -> (o1.lastComment.commentId - o2.lastComment.commentId).toInt()
        }
    }
}
