package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries


/**
 * [TicketEntry] implementation that is used for rendering welcome message of a ticket feed.
 * See [ServiceDeskConfiguration]
 *
 * @param message text that is rendered in UI
 */
internal class WelcomeMessageEntry(val message: String): TicketEntry() {
    override val type: Type = Type.WelcomeMessage
}