package com.pyrus.pyrusservicedesk._ref

import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFragment

object Screens {

    fun TicketScreen() = FragmentScreen {
        TicketFragment.newInstance()
    }

    fun ImageScreen() = FragmentScreen {
        TODO()
    }


}