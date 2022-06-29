package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

internal class ButtonsEntry(val buttons: List<String>, val onButtonClick: (text: String)-> Unit): TicketEntry() {
    override val type: Type = Type.Buttons
}