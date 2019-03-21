package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries

@FunctionalInterface
internal interface OnClickedCallback<in Item> {
    fun onClicked(item: Item)
}