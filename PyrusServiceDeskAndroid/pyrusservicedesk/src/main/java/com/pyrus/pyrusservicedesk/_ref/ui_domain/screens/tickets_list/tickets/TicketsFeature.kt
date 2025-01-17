package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

internal typealias TicketsFeature = Store<State, Message, Effect.Outer>

internal interface TicketsContract {

    data class State(
        val account: Account.V3,
        val contentState: ContentState,
    )

    sealed interface ContentState {

        data object Loading : ContentState

        data object Error : ContentState

        data class Content(
            val appId: String?,
            val titleText: String?,
            val titleImageUrl: String?,
            val filterId: String?,
            val filterName: String?,
            val filterEnabled: Boolean,
            val ticketSets: List<TicketSetInfo>?,
        ) : ContentState
    }

    sealed interface Message {

        sealed interface Outer : Message {
            data class OnFilterClick(val selectedUserId: String) : Outer
            data object OnRetryClick : Outer
            data object OnFabItemClick : Outer
            data class OnChangePage(val appId: String) : Outer
            data object OnCreateTicketClick : Outer

            data class OnTicketClick(val ticketId: Long, val userId: String) : Outer

            data class OnUserIdSelected(val userId: String) : Outer
        }

        sealed interface Inner : Message {
            data class TicketsUpdated(val tickets: TicketsInfo?) : Inner
            data object UpdateTicketsFailed : Inner
            data class UpdateTicketsCompleted(val tickets: TicketsInfo) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            data class ShowFilterMenu(
                val appId: String,
                val selectedUserId: String,
                val users: List<User>
            ) : Outer

            data class ShowAddTicketMenu(val appId: String, val users: List<User>) : Outer

            data object OpenQrFragment : Outer

            data object OpenSettingsFragment : Outer

        }

        sealed interface Inner : Effect {
            data object TicketsSetFlow : Inner
            data class UpdateTickets(val force: Boolean) : Inner
            data class OpenTicketScreen(val user: UserInternal, val ticketId: Long?) : Inner
        }
    }

}