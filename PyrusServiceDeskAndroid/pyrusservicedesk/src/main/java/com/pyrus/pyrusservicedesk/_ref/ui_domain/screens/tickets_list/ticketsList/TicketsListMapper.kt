package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListView.Effect.ShowAddTicketMenu
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListView.Effect.ShowTicket

internal object TicketsListMapper {

    fun map(effect: Effect): TicketsListView.Effect = when(effect) {
        is Effect.Outer.ShowAddTicketMenu -> ShowAddTicketMenu(effect.appId)
        is Effect.Outer.ShowTicket -> ShowTicket(
            effect.ticketId,
            effect.userId
        )

        Effect.Inner.TicketsAutoUpdate -> TODO()
        Effect.Inner.UpdateTickets -> TODO()
    }
}
