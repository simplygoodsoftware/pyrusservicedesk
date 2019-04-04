package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries


/**
 * Base class for entries that are used for rendering list of ticket/feed comments.
 */
internal abstract class TicketEntry {
    /**
     * Type of the ticket entry. See [Type].
     */
     abstract val type: Type
}

/**
 * Available types of entries. Used for speed optimization to avoid class casting.
 * TODO check the possibility of using sealed classes for this purpose.
 */
internal enum class Type {
    Comment,
    Date,
    WelcomeMessage
}