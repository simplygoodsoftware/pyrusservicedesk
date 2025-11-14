package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search

import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.State
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

internal typealias SearchFeature = Store<State, Message, Effect.Out>

interface SearchContract {

    data class State(
        val currentSearchText: String,
        val suggestions: List<SearchResult>,
        val requestSearchText: String,
    )

    sealed interface Message {

        sealed interface Out : Message {
            data class OnSearchChanged(val text: String) : Out

            data object OnCloseClick : Out

            data class OnTicketClick(
                val ticketId: Long,
                val commentId: Long?,
                val userId: String,
            ) : Out
        }

        sealed interface In : Message {
            data class SearchSuccess(val suggestions: List<SearchResult>, val query: String) : In
            data object Exit : In
            data class OpenTicket(
                val ticketId: Long,
                val commentId: Long?,
                val user: UserInternal,
            ) : In
        }


    }

    sealed interface Effect {

        sealed interface In : Effect {
            data class SearchText(val text: String) : In
            data object CloseScreen : In
            data class OpenTicket(
                val ticketId: Long,
                val commentId: Long?,
                val userId: String,
            ) : In
        }

        sealed interface Out : Effect {
            data object CloseKeyBoard : Out
            data object Exit : Out
            data class OpenTicket(
                val ticketId: Long,
                val commentId: Long?,
                val user: UserInternal,
            ) : Out
        }

    }

}

data class SearchResult(
    val ticketId: Long,
    val userId: String,
    val commentId: Long?,
    val title: String,
    val commentInfo: TicketHeader.LastComment?,
    val creationTime: Long?,
)