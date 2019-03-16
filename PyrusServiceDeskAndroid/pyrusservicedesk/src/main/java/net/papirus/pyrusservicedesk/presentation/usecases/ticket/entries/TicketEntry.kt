package net.papirus.pyrusservicedesk.presentation.usecases.ticket.entries

internal abstract class TicketEntry {
     abstract val type: Type
}

internal enum class Type {
    Comment,
    Date
}