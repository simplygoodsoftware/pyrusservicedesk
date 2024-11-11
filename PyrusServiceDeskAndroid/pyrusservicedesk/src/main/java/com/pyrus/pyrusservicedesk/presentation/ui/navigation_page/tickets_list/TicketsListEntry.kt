package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list


/**
 * Base class for entries that are used for rendering list of ticket/feed comments.
 */
internal abstract class TicketsListEntry {
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
    WelcomeMessage,
    Rating,
    Buttons
}