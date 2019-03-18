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
