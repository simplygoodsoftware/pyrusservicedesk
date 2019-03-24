package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

internal class WelcomeMessageEntry(val message: String): TicketEntry() {
    override val type: Type = Type.WelcomeMessage
}