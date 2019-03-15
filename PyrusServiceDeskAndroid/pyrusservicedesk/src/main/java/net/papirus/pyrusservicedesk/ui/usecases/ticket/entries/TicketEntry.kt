package net.papirus.pyrusservicedesk.ui.usecases.ticket.entries

internal abstract class TicketEntry {
     abstract val type: Type
}

internal enum class Type {
    Comment,
    Date
}