package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets_list

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets_list.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets_list.TicketsListContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets_list.TicketsListContract.State
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto

internal typealias TicketsListFeature = Store<State, Message, Effect.Outer>

internal interface TicketsListContract {

    /**
     * tickets - список тикетов для конкретного вендора
     */
    data class State(
        val allTickets: List<TicketDto>,
        val tickets: List<TicketDto>,
        val appId: String,
        val selectedUser: String,
        val isLoading: Boolean,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            data object OnCreateTicketClick : Outer

            data class OnTicketClick(val ticketId: Int) : Outer

            data class OnUserIdSelect(val userId: String) : Outer
        }

        sealed interface Inner : Message {

            data class TicketsUpdated(val tickets: TicketsDto) : Inner
            data class UserIdSelected(val userId: String) : Inner
            data object UpdateTicketsFailed : Inner
            data class UpdateTicketsCompleted(val tickets: TicketsDto) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            class ShowAddTicketMenu(val appId: String) : Outer
            class ShowTicket(val ticketId: Int?, val userId: String? = null) : Outer
        }

        sealed interface Inner : Effect {
            data object UpdateTickets : Inner
        }
    }

}