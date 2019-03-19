package net.papirus.pyrusservicedesk.ui.navigation

import net.papirus.pyrusservicedesk.ui.ActivityBase
import net.papirus.pyrusservicedesk.ui.usecases.file_preview.FilePreviewActivity
import net.papirus.pyrusservicedesk.ui.usecases.ticket.TicketActivity
import net.papirus.pyrusservicedesk.ui.usecases.tickets.TicketsActivity

internal class UiNavigator {

    companion object {
        fun toTickets(source: ActivityBase) {
            source.startActivity(TicketsActivity.getLaunchIntent())
        }

        fun toNewTicket(source: ActivityBase, unreadTicketCounter: Int) {
            source.startActivity(TicketActivity.getLaunchIntent(unreadCount = unreadTicketCounter))
        }

        fun toTicket(source: ActivityBase, ticketId: Int, unreadTicketCounter: Int) {
            source.startActivity(TicketActivity.getLaunchIntent(ticketId, unreadTicketCounter))
        }

        fun toFilePreview(source: ActivityBase, fileId: Int, fileName: String) {
            source.startActivity(FilePreviewActivity.getLaunchIntent(fileId, fileName))
        }
    }
}
