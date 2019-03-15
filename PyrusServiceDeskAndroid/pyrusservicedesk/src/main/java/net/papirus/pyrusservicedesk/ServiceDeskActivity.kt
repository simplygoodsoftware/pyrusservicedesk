package net.papirus.pyrusservicedesk

import android.content.Intent
import net.papirus.pyrusservicedesk.ui.usecases.ticket.TicketActivity
import net.papirus.pyrusservicedesk.ui.usecases.tickets.TicketsActivity

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