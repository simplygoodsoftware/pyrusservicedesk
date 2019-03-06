package net.papirus.pyrusservicedesk

import android.content.Intent
import net.papirus.pyrusservicedesk.ui.usecases.tickets.TicketsActivity

class ServiceDeskActivity {
    companion object {
        @JvmStatic()
        fun createIntent(): Intent {
            return Intent(
                    PyrusServiceDesk.getInstance().application,
                    TicketsActivity::class.java)
        }
    }
}