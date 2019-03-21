package net.papirus.pyrusservicedesk.presentation.ui.navigation

import net.papirus.pyrusservicedesk.presentation.ActivityBase
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewActivity
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsActivity

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
