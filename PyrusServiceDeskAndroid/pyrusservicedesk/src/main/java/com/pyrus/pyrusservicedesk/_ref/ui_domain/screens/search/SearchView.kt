package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search

import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

internal interface SearchView {

    data class Model(
        val searchResults: List<SearchResultEntry>?,
        val search: String,
        val showEmptyPage: Boolean,
        val emptyPageText: TextProvider?,
    )

    sealed interface Event {
        data class OnSearchChanged(val text: String) : Event
        data class OnTicketClick(
            val ticketId: Long,
            val commentId: Long?,
            val userId: String,
        ) : Event

        data object OnCloseClick : Event
    }

    sealed interface Effect {
        data object CloseKeyboard : Effect
        data object Exit : Effect
        data class OpenTicket(
            val ticketId: Long,
            val commentId: Long?,
            val user: UserInternal,
        ) : Effect
    }

}

internal data class SearchResultEntry(
    val ticketId: Long,
    val commentId: Long?,
    val userId: String,
    val title: String,
    val description: TextProvider?,
    val query: String,
    val commentCreationTime: Long?,
)