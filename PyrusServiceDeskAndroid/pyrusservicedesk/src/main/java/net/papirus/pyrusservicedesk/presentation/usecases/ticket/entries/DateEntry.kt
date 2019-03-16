package net.papirus.pyrusservicedesk.presentation.usecases.ticket.entries

internal class DateEntry(val date: String): TicketEntry() {
    override val type: Type = Type.Date
}