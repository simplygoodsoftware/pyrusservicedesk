package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.Effect.Outer
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider

internal interface TicketsListView {

    sealed interface Effect {
        class ShowAddTicketMenu(val appId: String) : Effect
        class ShowTicket(val ticketId: Int?, val userId: String? = null) : Effect
    }
}