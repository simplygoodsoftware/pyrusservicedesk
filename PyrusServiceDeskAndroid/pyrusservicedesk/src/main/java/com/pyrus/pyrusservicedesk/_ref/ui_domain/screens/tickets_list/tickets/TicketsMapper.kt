package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry

internal object TicketsMapper {

    var userId: String? = null

    fun map(state: ContentState): Model = when(state) {
        is ContentState.Content -> {
            userId = state.filterId
            Model(
                titleText = state.titleText,
                titleImageUrl = state.titleImageUrl,
                filterName = state.filterName,
                ticketsIsEmpty = state.ticketSets.isNullOrEmpty(),
                filterEnabled = state.filterEnabled,
                tabLayoutIsVisibile = if (state.ticketSets != null) state.ticketSets.size > 1 else false,
                ticketSets = state.ticketSets?.map(::map) ?: emptyList(),
                showNoConnectionError = false,
                isLoading = false,
            )
        }
        ContentState.Error -> Model(
            titleText = null,
            titleImageUrl = null,
            filterName = null,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutIsVisibile = false,
            ticketSets = null,
            showNoConnectionError = true,
            isLoading = false,
        )
        ContentState.Loading -> Model(
            titleText = null,
            titleImageUrl = null,
            filterName = null,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutIsVisibile = false,
            ticketSets = null,
            showNoConnectionError = false,
            isLoading = true,
        )
    }

    private fun map(ticketSetInfo: TicketSetInfo): TicketSetInfoEntry {

        val filteredTickets =
            if (userId == null) ticketSetInfo.tickets
            else ticketSetInfo.tickets.filter { it.userId == userId }

        return TicketSetInfoEntry(
            appId = ticketSetInfo.appId,
            titleText = ticketSetInfo.orgName,
            tickets = filteredTickets.map(::map),
        )
    }

    private fun map(header: TicketHeader): Model.TicketHeaderEntry = Model.TicketHeaderEntry(
        ticketId = header.ticketId,
        userId = header.userId,
        title = header.subject,
        lastCommentText = header.lastCommentText,
        lastCommentCreationTime = header.lastCommentCreationDate,
        isRead = header.isRead
    )
}
