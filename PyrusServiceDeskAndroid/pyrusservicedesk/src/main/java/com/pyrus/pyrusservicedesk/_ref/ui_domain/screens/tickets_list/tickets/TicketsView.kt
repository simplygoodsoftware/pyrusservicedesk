package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.utils.TextProvider


internal interface TicketsView {

    data class Model(
        val titleText: String?,
        val titleImageUrl: String?,
        val filterName: String?,
        val ticketsIsEmpty: Boolean,
        val filterEnabled: Boolean,
        val tabLayoutIsVisible: Boolean,
        val ticketSetInfo: TicketSetInfo?,
        val showNoConnectionError: Boolean,
        val isLoading: Boolean,
    ) {

        data class TicketSetInfo(
            val page: Int,
            val ticketSets: List<TicketSetInfoEntry>?,
        )

        data class TicketSetInfoEntry(
            val appId: String,
            val titleText: String,
            val tickets: List<TicketHeaderEntry>,
            val isUserTriggerLoading: Boolean,
            val isLoading: Boolean,
        )

        data class TicketHeaderEntry(
            val ticketId: Long,
            val userId: String,
            val title: String?,
            val lastCommentText: TextProvider?,
            val lastCommentCreationTime: Long?,
            val isRead: Boolean,
            val isLoading: Boolean,
        )
    }

}