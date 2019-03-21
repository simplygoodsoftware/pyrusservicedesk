package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

internal class DateEntry(val date: String): TicketEntry() {
    override val type: Type = Type.Date
}