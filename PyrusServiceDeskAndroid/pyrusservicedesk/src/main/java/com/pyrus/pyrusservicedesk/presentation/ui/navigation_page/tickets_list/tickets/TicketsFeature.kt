package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import androidx.fragment.app.FragmentManager
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets

internal typealias TicketsFeature = Store<State, Message, Effect>

internal interface TicketsContract {

    data class State(
        val appId: String,
        val titleText: String,
        val titleImageUrl: String,
        val filterName: String,
        val ticketsIsEmpty: Boolean,
        val filterEnabled: Boolean,
        val tickets: Tickets,
        val isLoading: Boolean,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            data class OnFilterClick(
                val users: HashMap<String, String>,
                val selectedUserId: String
            ) : Outer

            object OnScanClick : Outer

            object OnSettingsClick : Outer

            data class OnFabItemClick(val users: HashMap<String, String>) : Outer

        }

        sealed interface Inner : Message {

            data class TicketsUpdated(val tickets: Tickets) : Inner
            data class UserIdSelected(val userId: String, val fm: FragmentManager) : Inner
            object UpdateTicketsFailed : Inner
            object UpdateTicketsCompleted : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            class ShowFilterMenu(
                val appId: String,
                val selectedUserId: String
            ) : Outer

            class ShowAddTicketMenu(val appId: String) : Outer

            class ShowFilterSelected(val filterName: String) : Outer

            class ShowTicket(val ticketId: Int = 0, val userId: String? = null) : Outer
        }

        sealed interface Inner : Effect {
            object UpdateTickets : Inner
            //object FeedFlow : Inner
            object TicketsAutoUpdate : Inner
        }
    }

}