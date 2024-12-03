package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries

/**
 * Interface that can be used for detecting user clicks on views that hold the data
 * of the [Item] type
 */
@FunctionalInterface
internal interface OnClickedCallback<in Item> {
    /**
     * Callback to be invoked when view that holds an [item] was clicked.
     */
    fun onClicked(item: Item)
}