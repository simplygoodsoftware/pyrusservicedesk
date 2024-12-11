package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.State
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets

internal typealias TicketsListFeature = Store<State, Message, Effect>

internal interface TicketsListContract {

    /**
     * tickets - список тикетов для конкретного вендора
     */
    data class State(
        val allTickets: List<Ticket>,
        val tickets: List<Ticket>,
        val appId: String,
        val users: HashMap<String, String>,
        val selectedUser: String,
        val isLoading: Boolean,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            object OnCreateTicketClick : Outer

            data class OnTicketClick(val ticketId: Int) : Outer
        }

        sealed interface Inner : Message {

            data class TicketsUpdated(val tickets: Tickets) : Inner
            data class UserIdSelected(val userId: String) : Inner
            object UpdateTicketsFailed : Inner
            object UpdateTicketsCompleted : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            class ShowAddTicketMenu(val appId: String) : Outer
            class ShowTicket(val ticketId: Int = 0, val userId: String? = null) : Outer
        }

        sealed interface Inner : Effect {
            object UpdateTickets : Inner
            //object FeedFlow : Inner
            object TicketsAutoUpdate : Inner
        }
    }

}