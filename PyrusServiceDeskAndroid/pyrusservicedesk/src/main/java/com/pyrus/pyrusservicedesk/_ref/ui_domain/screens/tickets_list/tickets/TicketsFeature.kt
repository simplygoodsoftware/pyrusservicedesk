package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

internal typealias TicketsFeature = Store<State, Message, Effect.Outer>

internal interface TicketsContract {

    data class State(
        val contentState: ContentState,
    )

    sealed interface ContentState {
        data object Loading : ContentState
        data object Error : ContentState
        data class Content(
            val account: Account,
            val pageAppId: String?,
            val filter: User?,
            val ticketSets: List<TicketSetInfo>?,
            val isUserTriggerLoading: Boolean,
            val loadUserIds: Set<String>,
        ) : ContentState
    }

    sealed interface Message {

        sealed interface Outer : Message {
            data class OnFilterClick(val selectedUserId: String) : Outer
            data object OnRetryClick : Outer
            data object OnRefresh : Outer
            data object OnFabItemClick : Outer
            data class OnChangePage(val appId: String) : Outer
            data object OnCreateTicketClick : Outer
            data class OnTicketClick(val ticketId: Long, val userId: String) : Outer
            data class OnFilterSelected(val userId: String) : Outer
            data object OnUsersIsEmpty : Outer
        }

        sealed interface Inner : Message {
            data class TicketsUpdated(val ticketsInfo: TicketsInfo) : Inner
            data object UpdateTicketsFailed : Inner
            data class UpdateTicketsCompleted(val ticketsInfo: TicketsInfo, val filter: User?, val showAccessDenied: Boolean) : Inner
            data class OnDialogAccessDenied(val message: TextProvider, val usersIsEmpty: Boolean) : Inner
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
            data class ShowDialog(val message: TextProvider, val usersIsEmpty: Boolean) : Outer
        }

        sealed interface Inner : Effect {
            data object TicketsSetFlow : Inner
            data object EventsFlow : Inner
            data class UpdateTickets(val force: Boolean) : Inner
            data class OpenTicketScreen(val user: UserInternal, val ticketId: Long?) : Inner
            data object Close : Inner
        }
    }

}