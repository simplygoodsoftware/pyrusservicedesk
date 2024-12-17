package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

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
}