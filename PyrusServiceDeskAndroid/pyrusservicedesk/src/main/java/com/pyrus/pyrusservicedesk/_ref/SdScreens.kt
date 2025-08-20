package com.pyrus.pyrusservicedesk._ref

import com.github.terrakok.cicerone.androidx.ActivityScreen
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk.OpenTicketAction
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.file_preview.FilePreviewActivity
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.rootFragment.RootFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.rootFragment.RouterFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

@Suppress("FunctionName")
internal object SdScreens {

    fun TicketScreen(ticketId: Long, user: UserInternal) = FragmentScreen(clearContainer = false) {
        TicketFragment.newInstance(ticketId, null, user)
    }

    fun TicketScreen(ticketId: Long, commentId: Long?, user: UserInternal) = FragmentScreen(clearContainer = false) {
        TicketFragment.newInstance(ticketId, commentId, user)
    }

    fun TicketsScreen() = FragmentScreen {
        TicketsFragment.newInstance()
    }

    fun ImageScreen(fileData: FileData) = ActivityScreen {
        FilePreviewActivity.getLaunchIntent(it, fileData)
    }

    fun SearchScreen() = FragmentScreen(clearContainer = false) {
       SearchFragment.newInstance()
    }

    fun RootScreen(openTicketAction: OpenTicketAction?) = FragmentScreen {
        RootFragment.newInstance(openTicketAction)
    }

    fun RouterScreen(openTicketAction: OpenTicketAction?) = FragmentScreen {
        RouterFragment.newInstance(openTicketAction)
    }

}