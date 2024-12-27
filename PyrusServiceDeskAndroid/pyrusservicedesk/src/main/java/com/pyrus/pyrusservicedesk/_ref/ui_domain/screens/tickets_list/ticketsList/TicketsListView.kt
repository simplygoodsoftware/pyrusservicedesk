package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList

internal interface TicketsListView {

    sealed interface Effect {
        class ShowAddTicketMenu(val appId: String) : Effect
        class ShowTicket(val ticketId: Int?, val userId: String? = null) : Effect
    }
}