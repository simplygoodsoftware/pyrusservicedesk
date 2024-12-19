package com.pyrus.pyrusservicedesk._ref

import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment

object Screens {

    const val KEY_DEFAULT_USER_ID = "0"
    const val KEY_DEFAULT_TICKET_ID = 0

    fun TicketScreen(ticketId: Int = KEY_DEFAULT_TICKET_ID, userId: String = KEY_DEFAULT_USER_ID) = FragmentScreen {
        TicketFragment.newInstance(ticketId, userId)
    }

    fun TicketsScreen() = FragmentScreen {
        TicketsFragment.newInstance()
    }

    fun ImageScreen() = FragmentScreen {
        TODO()
    }


}