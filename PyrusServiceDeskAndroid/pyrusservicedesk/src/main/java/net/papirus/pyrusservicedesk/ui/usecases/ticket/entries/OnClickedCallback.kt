package net.papirus.pyrusservicedesk.ui.usecases.ticket.entries

@FunctionalInterface
internal interface OnClickedCallback<in Item> {
    fun onClicked(item: Item)
}