package com.pyrus.pyrusservicedesk._ref

import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsFragment

object Screens {

    fun TicketsScreen() = FragmentScreen {
        TicketsFragment.newInstance()
    }


}