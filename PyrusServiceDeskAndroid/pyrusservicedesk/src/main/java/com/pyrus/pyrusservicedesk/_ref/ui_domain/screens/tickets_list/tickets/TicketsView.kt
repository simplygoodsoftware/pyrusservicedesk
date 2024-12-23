package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.data.FullTicket

internal interface TicketsView {

    data class Model(
        val titleText: String,
        val titleImageUrl: String?,
        val filterName: String?,
        val ticketsIsEmpty: Boolean,
        val filterEnabled: Boolean,
        val tabLayoutVisibility: Boolean,
        val applications: List<TicketSetInfoEntry>,
        val showNoConnectionError: Boolean,
        val isLoading: Boolean,
    ) {
        data class TicketSetInfoEntry(
            val appId: String,
            val titleText: String,
            val tickets: List<FullTicket>,
        )
    }

}