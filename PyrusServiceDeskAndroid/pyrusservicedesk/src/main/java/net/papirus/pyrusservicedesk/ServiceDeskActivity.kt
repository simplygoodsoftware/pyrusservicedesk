package net.papirus.pyrusservicedesk

import android.content.Intent
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsActivity

class ServiceDeskActivity {
    companion object {
        @JvmStatic
        fun createIntent(): Intent {
            return when(PyrusServiceDesk.getInstance().enableRichUi){
                true -> TicketsActivity.getLaunchIntent()
                else -> TicketActivity.getLaunchIntent()
            }
        }
    }
}