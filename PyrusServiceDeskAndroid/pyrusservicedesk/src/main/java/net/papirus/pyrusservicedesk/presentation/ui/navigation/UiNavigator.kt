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

        fun toNewTicket(source: ActivityBase) {
            source.startActivity(TicketActivity.getLaunchIntent())
        }

        fun toTicket(source: ActivityBase, ticketId: Int) {
            source.startActivity(TicketActivity.getLaunchIntent(ticketId))
        }

        fun toFilePreview(source: ActivityBase, fileId: Int) {
            source.startActivity(FilePreviewActivity.getLaunchIntent(fileId))
        }
    }
}