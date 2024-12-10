package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets

internal typealias TicketsFeature = Store<State, Message, Effect>

internal interface TicketsContract {

    data class State(
        val tickets: Tickets,
        val isLoading: Boolean,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            object OnTitleClick : Outer

            data class OnFilterClick(
                val users: HashMap<String, String>,
                val selectedUserId: String
            ) : Outer

            object OnDeleteFilterClick : Outer

            data class OnScanClick(val text: String) : Outer

            data class OnSettingsClick(val rating: Int) : Outer

            data class OnFabItemClick(val users: HashMap<String, String>) : Outer

            data class OnTicketsClick(val userId: String, val ticketId: Int) : Outer

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
            class ShowFilterMenu(
                val users: HashMap<String, String>,
                val selectedUserId: String
            ) : Outer

            class ShowAddTicketMenu(val users: HashMap<String, String>) : Outer

            class ShowTicket(val ticketId: Int = 0, val userId: String? = null) : Outer
        }

        sealed interface Inner : Effect {
            object UpdateTickets : Inner
            //object FeedFlow : Inner
            object TicketsAutoUpdate : Inner
        }
    }

}