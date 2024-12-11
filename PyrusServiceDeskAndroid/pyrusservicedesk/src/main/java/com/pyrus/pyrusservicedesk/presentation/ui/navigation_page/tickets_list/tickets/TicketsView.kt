package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets

internal interface  TicketsView {
    data class TicketListModel(
        val titleText: String,
        val titleImageUrl: String,
        val filterName: String,
        val ticketsIsEmpty: Boolean,
        val filterEnabled: Boolean,
    )

    sealed interface Event {

        object OnTitleClick : Event

        object OnFilterClick : Event

        data class OnScanClick(val text: String) : Event

        data class OnSettingsClick(val rating: Int) : Event

        object OnFabItemClick : Event

        object OnTicketsClick : Event

    }
}