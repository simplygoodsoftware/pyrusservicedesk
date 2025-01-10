package com.pyrus.pyrusservicedesk._ref

import com.github.terrakok.cicerone.androidx.ActivityScreen
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.file_preview.FilePreviewActivity
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

internal object Screens {

    fun TicketScreen(ticketId: Int?, user: UserInternal) = FragmentScreen {
        TicketFragment.newInstance(ticketId, user)
    }

    fun TicketsScreen() = FragmentScreen {
        TicketsFragment.newInstance()
    }

    fun ImageScreen(fileData: FileData) = ActivityScreen {
        FilePreviewActivity.getLaunchIntent(it, fileData)
    }


}