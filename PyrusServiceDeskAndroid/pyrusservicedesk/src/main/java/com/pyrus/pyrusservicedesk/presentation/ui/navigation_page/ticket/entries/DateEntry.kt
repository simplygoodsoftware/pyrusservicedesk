package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

/**
 * Entry that is used for rendering the date group of the following comments.
 *
 * @param date localized name of the group to be rendered by UI.
 */
internal class DateEntry(val date: String): TicketEntry() {
    override val type: Type = Type.Date
}