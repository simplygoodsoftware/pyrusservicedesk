package com.pyrus.pyrusservicedesk.presentation.ui.navigation

import com.pyrus.pyrusservicedesk.presentation.ActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsActivity
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData

/**
 * Handles all supported navigation through main screens.
 */
internal class UiNavigator {

    companion object {

        /**
         * Navigates to the screen with list of available tickets.
         *
         * @param source activity to navigate from
         */
        fun toTickets(source: ActivityBase) {
            source.startActivity(TicketsActivity.getLaunchIntent())
        }

        /**
         * Navigates to the screen where new ticket can be created.
         *
         * @param source activity to navigate from
         * @param unreadTicketCounter current amount of unread tickets
         */
        fun toNewTicket(source: ActivityBase, unreadTicketCounter: Int) {
            source.startActivity(TicketActivity.getLaunchIntent(unreadCount = unreadTicketCounter))
        }

        /**
         * Navigates to the ticket with the given id.
         *
         * @param source activity to navigate from
         * @param ticketId id of ticket to be navigated to
         * @param unreadTicketCounter current amount of unread tickets
         */
        fun toTicket(source: ActivityBase, ticketId: Int, unreadTicketCounter: Int) {
            source.startActivity(TicketActivity.getLaunchIntent(ticketId, unreadTicketCounter))
        }

        /**
         * Navigates to the screen that is responsible for previewing files.
         *
         * @param source activity to navigate from
         * @param fileData data of the attachment to be used for previewing
         */
        fun toFilePreview(source: ActivityBase, fileData: FileData) {
            source.startActivity(FilePreviewActivity.getLaunchIntent(fileData))
        }
    }
}
