package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

internal class ButtonsEntry(
    val buttons: List<ButtonEntry>,
    val onButtonClick: (text: String) -> Unit,
) : TicketEntry() {
    override val type: Type = Type.Buttons
}

internal sealed interface ButtonEntry {

    val text: String

    data class Simple(
        override val text: String,
    ) : ButtonEntry

    data class Link(
        override val text: String,
        val link: String,
    ) : ButtonEntry

}