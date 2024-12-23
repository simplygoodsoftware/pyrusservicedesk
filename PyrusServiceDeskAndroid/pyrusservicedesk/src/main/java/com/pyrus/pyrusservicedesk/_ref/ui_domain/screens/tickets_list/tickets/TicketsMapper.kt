package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry

internal object TicketsMapper {

    fun map(state: State): Model {
        return Model(
            titleText = state.titleText ?: "",
            titleImageUrl = state.titleImageUrl,
            filterName = state.filterName,
            ticketsIsEmpty = state.tickets.isNullOrEmpty(),
            filterEnabled = state.filterEnabled,
            tabLayoutVisibility = if (state.tickets != null) state.tickets.size > 1 else false,
            applications = state.tickets?.map(::map) ?: emptyList(),
            showNoConnectionError = state.showNoConnectionError,
            isLoading = state.isLoading,
        )
    }

    private fun map(ticketSetInfo: TicketSetInfo): TicketSetInfoEntry = TicketSetInfoEntry(
        appId = ticketSetInfo.appId,
        titleText = ticketSetInfo.orgName,
        tickets = ticketSetInfo.tickets, // TODO map
    )
}
