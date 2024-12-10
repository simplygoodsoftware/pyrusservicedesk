package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.State
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory2,
    private val actor: TicketsListActor,
    private val baseRepository: SyncRepository
) {

    fun create(): TicketsListFeature = storeFactory.create(
        name = TAG,
        initialState = State(
            tickets = Tickets(
                null,
                commandsResult = emptyList()
            ),
            isLoading = true
        ),
        reducer = FeatureReducer(),
        actor = actor.adaptCast(),
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
                    Effect.Outer.ShowAddTicketMenu(message.users)
                }
                else if (message.users.isEmpty()){
                    Effect.Outer.ShowTicket()
                }
                else {
                    Effect.Outer.ShowTicket(userId = message.users[message.users.keys.first()])
                }
            }

            is Message.Outer.OnFilterClick -> {
                Effect.Outer.ShowFilterMenu(message.users, message.selectedUserId)
            }

            is Message.Outer.OnScanClick -> TODO()

            is Message.Outer.OnSettingsClick -> TODO()

            is Message.Outer.OnTicketsClick -> {
                Effect.Outer.ShowTicket(message.ticketId, message.userId)
            }

            Message.Outer.OnTitleClick -> TODO()
            Message.Outer.OnDeleteFilterClick -> TODO()
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            Message.Inner.UpdateTicketsFailed -> TODO()
            Message.Inner.UpdateTicketsCompleted -> TODO()
            is Message.Inner.TicketsUpdated -> TODO()
            is Message.Inner.UserIdSelected -> TODO()
        }
    }

}

internal class TicketsListActor(
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
