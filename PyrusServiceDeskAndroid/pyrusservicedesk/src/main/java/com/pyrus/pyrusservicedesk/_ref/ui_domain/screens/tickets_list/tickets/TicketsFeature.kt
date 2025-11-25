package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.app.Activity
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

internal typealias TicketsFeature = Store<State, Message, Effect.Outer>

interface TicketsContract {

    data class State(
        val contentState: ContentState,
    )

    sealed interface ContentState {
        data object Loading : ContentState
        data object Error : ContentState
        data class Content(
            val users: List<User>,
            val pageAppId: String?,
            val filter: User?,
            val closedTicketsIsExpanded: Boolean,
            val ticketSets: List<TicketSetInfo>?,
            val isUserTriggerLoading: Boolean,
            val userWithData: List<User>,
        ) : ContentState
    }

    sealed interface Message {

        sealed interface Outer : Message {
            data object OnFilterClick : Outer
            data object OnRetryClick : Outer
            data object OnRefresh : Outer
            data object OnFabItemClick : Outer
            data object OnClosedTicketsTitleCLick: Outer
            data class OnChangePage(val appId: String) : Outer
            data object OnCreateTicketClick : Outer
            data class OnTicketClick(val ticketId: Long, val userId: String) : Outer
            data class OnFilterSelected(val userId: String) : Outer
            data class OnDialogPositiveButtonClick(val activity: Activity, val goBack: Boolean) : Outer
            data object OnRightButtonClick : Outer
            data object OnSearchClick : Outer
        }

        sealed interface Inner : Message {
            data object UpdateTicketsFailed : Inner
            data class UpdateTicketsCompleted(
                val ticketsInfo: TicketsInfo,
                val pendingFilter: User?,
            ) : Inner

            data class OpenTicket(
                val ticketId: Long,
                val commentId: Long?,
                val user: UserInternal) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            data class ShowFilterMenu(
                val appId: String,
                val selectedUserId: String?,
                val users: List<User>
            ) : Outer
            data object ScrollToClosedHeader : Outer
            data object ScrollUp : Outer
            data class ShowAddTicketMenu(val appId: String, val users: List<User>) : Outer
            data class OpenTicket(
                val ticketId: Long,
                val commentId: Long?,
                val user: UserInternal
            ) : Outer
        }

        sealed interface Inner : Effect {
            data object TicketsSetFlow : Inner
            data object AddUserEventFlow : Inner
            data class UpdateTickets(val force: Boolean) : Inner
            data class OpenTicketScreen(val user: UserInternal, val ticketId: Long?) : Inner
            data object Close : Inner
            data object OpenRightButtonScreen : Inner
            data object OpenSearchScreen : Inner
            data object UpdateAudioData : Inner
            data object CheckExtraUsers : Inner
        }
    }

}