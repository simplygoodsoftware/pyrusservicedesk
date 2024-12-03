package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries


/**
 * [TicketEntry] implementation that is used for rendering welcome message of a ticket feed.
 * See [ServiceDeskConfiguration]
 *
 * @param message text that is rendered in UI
 */
internal class WelcomeMessageEntry(val message: String): TicketEntry() {
    override val type: Type = Type.WelcomeMessage
}