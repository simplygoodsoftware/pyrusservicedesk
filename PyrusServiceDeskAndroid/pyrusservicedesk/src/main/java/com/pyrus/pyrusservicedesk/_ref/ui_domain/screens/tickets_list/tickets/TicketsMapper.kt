package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.TicketListModel

internal object TicketsMapper {

    fun map(state: State): TicketListModel = TicketListModel(
        titleText = state.titleText,
        titleImageUrl = state.titleImageUrl,
        filterName = state.filterName,
        ticketsIsEmpty = state.ticketsIsEmpty,
        filterEnabled = state.filterEnabled,
        tabLayoutVisibility = state.tabLayoutVisibility,
        applications = state.applications,
    )
}
