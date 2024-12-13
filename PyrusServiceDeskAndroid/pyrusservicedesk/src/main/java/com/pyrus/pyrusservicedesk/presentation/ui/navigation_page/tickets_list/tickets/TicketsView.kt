package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import com.pyrus.pyrusservicedesk.sdk.data.Application

internal interface  TicketsView {
    data class TicketListModel(
        val titleText: String,
        val titleImageUrl: String,
        val filterName: String,
        val ticketsIsEmpty: Boolean,
        val filterEnabled: Boolean,
        val tabLayoutVisibility: Boolean,
        val applications: HashSet<Application>
    )

    sealed interface Event {

        data object OnTitleClick : Event

        data object OnFilterClick : Event

        data class OnScanClick(val text: String) : Event

        data class OnSettingsClick(val rating: Int) : Event

        data object OnFabItemClick : Event

        data object OnTicketsClick : Event

    }
}