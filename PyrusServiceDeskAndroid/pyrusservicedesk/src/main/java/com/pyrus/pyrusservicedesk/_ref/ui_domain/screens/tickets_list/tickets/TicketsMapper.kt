package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getOrganisationLogoUrl
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils

internal object TicketsMapper {

    fun map(state: ContentState): Model = when(state) {
        is ContentState.Content -> {

            val ticketSet = state.ticketSets?.map {
                map(it, state.isUserTriggerLoading, state.loadUserIds, state.filter)
            } ?: emptyList()

            var page = 0
            val ticketSets = state.ticketSets ?: emptyList()
            for (i in ticketSets.indices) {
                if(ticketSets[i].appId == state.pageAppId) {
                    page = i
                    break
                }
            }

            val ticketsSetByAppName = state.ticketSets?.associateBy { it.appId }
            val titleUrl = ticketsSetByAppName?.get(state.pageAppId)?.orgLogoUrl?.let {
                getOrganisationLogoUrl(it, state.account.domain)
            }
            val titleText = ticketsSetByAppName?.get(state.pageAppId)?.orgName

            Model(
                titleText = titleText,
                titleImageUrl = titleUrl,
                filterName = state.filter?.userName,
                ticketsIsEmpty = state.ticketSets.isNullOrEmpty(),
                filterEnabled = state.filter != null,
                tabLayoutIsVisible = state.account.getUsers().size > 1,
                ticketSetInfo = Model.TicketSetInfo(page, ticketSet),
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
            tabLayoutIsVisible = false,
            ticketSetInfo = null,
            showNoConnectionError = true,
            isLoading = false,
        )
        ContentState.Loading -> Model(
            titleText = null,
            titleImageUrl = null,
            filterName = null,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutIsVisible = false,
            ticketSetInfo = null,
            showNoConnectionError = false,
            isLoading = true,
        )
    }

    private fun map(
        ticketSetInfo: TicketSetInfo,
        isUserTriggerLoading: Boolean,
        loadUserIds: Set<String>,
        filter: User?,
    ): TicketSetInfoEntry {

        val isLoading =
            ticketSetInfo.userIds.size == 1 && ticketSetInfo.userIds.first() in loadUserIds
                || filter != null && filter.appId == ticketSetInfo.appId && filter.userId in loadUserIds

        val filteredTickets = when {
            isLoading -> emptyList()
            filter == null || filter.appId != ticketSetInfo.appId -> ticketSetInfo.tickets
            else -> ticketSetInfo.tickets.filter { it.userId == filter.userId }
        }

        return TicketSetInfoEntry(
            appId = ticketSetInfo.appId,
            titleText = ticketSetInfo.orgName,
            tickets = filteredTickets.map(::map),
            isUserTriggerLoading = isUserTriggerLoading,
            isLoading = isLoading
        )
    }

    private fun map(header: TicketHeader): Model.TicketHeaderEntry {
        val titleText = header.subject?.let(HtmlTagUtils::cleanTags)
        val lastCommentText = header.lastCommentText
        return Model.TicketHeaderEntry(
            ticketId = header.ticketId,
            userId = header.userId,
            title = titleText,
            lastCommentText = lastCommentText,
            lastCommentCreationTime = header.lastCommentCreationDate,
            isRead = header.isRead,
            isLoading = header.isLoading,
        )
    }
}
