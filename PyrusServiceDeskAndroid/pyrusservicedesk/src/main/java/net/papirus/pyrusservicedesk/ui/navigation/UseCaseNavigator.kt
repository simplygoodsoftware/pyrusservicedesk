package net.papirus.pyrusservicedesk.ui.navigation

import net.papirus.pyrusservicedesk.ui.ActivityBase
import net.papirus.pyrusservicedesk.ui.usecases.ticket.TicketActivity

internal class UseCaseNavigator {

    companion object {
        fun toNewTicket(source: ActivityBase) {
            TicketActivity.startNewTicket(source)
        }

        fun toTicket(source: ActivityBase, ticketId: Int) {
            TicketActivity.startTicket(source, ticketId)
        }
    }
}