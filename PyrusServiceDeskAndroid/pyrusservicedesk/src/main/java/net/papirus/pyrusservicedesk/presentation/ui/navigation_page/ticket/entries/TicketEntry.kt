package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

internal abstract class TicketEntry {
     abstract val type: Type
}

internal enum class Type {
    Comment,
    Date
}