package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets


internal interface TicketsView {

    data class Model(
        val titleText: String?,
        val titleImageUrl: String?,
        val filterName: String?,
        val ticketsIsEmpty: Boolean,
        val filterEnabled: Boolean,
        val tabLayoutVisibility: Boolean,
        val applications: List<TicketSetInfoEntry>?,
        val showNoConnectionError: Boolean,
        val isLoading: Boolean,
    ) {
        data class TicketSetInfoEntry(
            val appId: String,
            val titleText: String,
            val tickets: List<TicketHeaderEntry>,
        )

        data class TicketHeaderEntry(
            val ticketId: Long,
            val userId: String,
            val title: String?,
            val lastCommentText: String?,
            val lastCommentCreationTime: Long?,
            val isRead: Boolean,
        )
    }

}