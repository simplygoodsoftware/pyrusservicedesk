package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk._ref.utils.TextProvider


internal interface TicketsView {

    data class Model(
        val titleText: TextProvider?,
        val titleImageUrl: String?,
        val tabLayoutIsVisible: Boolean,
        val isUserTriggerLoading: Boolean,
        val tickets: List<TicketsEntry>,
        val appTabs: List<TabEntry>,
        val showNoConnectionError: Boolean,
        val isLoading: Boolean,
        val showCreateTicketPicture: Boolean,
    ) {

        data class TabEntry(
            val appId: String,
            val titleText: String,
            val isSelected: Boolean,
        )

        internal sealed interface TicketsEntry {

            data class ClosedTicketTitleEntry(
                val isExpanded: Boolean,
                val count: Int,
            ) : TicketsEntry

            data class TicketHeaderEntry(
                val ticketId: Long,
                val userId: String,
                val title: TextProvider,
                val lastCommentText: TextProvider?,
                val lastCommentIconRes: Int?,
                val lastCommentCreationTime: Long?,
                val isRead: Boolean,
                val isLoading: Boolean,
            ) : TicketsEntry
        }
    }

}