package net.papirus.pyrusservicedesk.presentation.usecases.ticket.entries

@FunctionalInterface
internal interface OnClickedCallback<in Item> {
    fun onClicked(item: Item)
}