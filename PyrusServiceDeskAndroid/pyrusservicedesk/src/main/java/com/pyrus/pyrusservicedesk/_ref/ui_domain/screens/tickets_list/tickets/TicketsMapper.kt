package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.*
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry

internal object TicketsMapper {

    fun map(state: ContentState): Model = when(state) {
        is ContentState.Content -> Model(
            titleText = state.titleText ?: "",
            titleImageUrl = state.titleImageUrl,
            filterName = state.filterName,
            ticketsIsEmpty = state.tickets.isNullOrEmpty(),
            filterEnabled = state.filterEnabled,
            tabLayoutVisibility = if (state.tickets != null) state.tickets.size > 1 else false,
            applications = state.tickets?.map(::map) ?: emptyList(),
            showNoConnectionError = false,
            isLoading = false,
        )
        ContentState.Error -> Model(
            titleText = null,
            titleImageUrl = null,
            filterName = null,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutVisibility = false,
            applications = null,
            showNoConnectionError = true,
            isLoading = false,
        )
        ContentState.Loading -> Model(
            titleText = null,
            titleImageUrl = null,
            filterName = null,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutVisibility = false,
            applications = null,
            showNoConnectionError = false,
            isLoading = true,
        )
    }

    private fun map(ticketSetInfo: TicketSetInfo): TicketSetInfoEntry = TicketSetInfoEntry(
        appId = ticketSetInfo.appId,
        titleText = ticketSetInfo.orgName,
        tickets = ticketSetInfo.tickets, // TODO map to entry
    )
}
