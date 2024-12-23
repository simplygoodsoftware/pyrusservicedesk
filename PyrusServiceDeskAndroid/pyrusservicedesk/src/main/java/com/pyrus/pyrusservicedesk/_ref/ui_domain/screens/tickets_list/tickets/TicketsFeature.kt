package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import androidx.fragment.app.FragmentManager
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk.sdk.data.ApplicationDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto

internal typealias TicketsFeature = Store<State, Message, Effect.Outer>

internal interface TicketsContract {

    data class State(
        val appId: String?,
        val titleText: String?,
        val titleImageUrl: String?,
        val filterName: String?,
        val filterEnabled: Boolean,
        val tickets: List<TicketSetInfo>?,
        val isLoading: Boolean,
        val showNoConnectionError: Boolean,
    )

    sealed interface Message {

        sealed interface Outer : Message {

            data class OnFilterClick(
                val selectedUserId: String
            ) : Outer

            data object OnScanClick : Outer

            data object OnSettingsClick : Outer

            data object OnFabItemClick : Outer

            data class OnChangeApp(val appId: String) : Outer

            data object OnCreateTicketClick : Outer

            data class OnTicketClick(val ticketId: Int) : Outer

            data class OnUserIdSelect(val userId: String) : Outer

        }

        sealed interface Inner : Message {

            data class TicketsUpdated(val tickets: TicketsInfo) : Inner
            data class UserIdSelected(val userId: String, val fm: FragmentManager) : Inner
            data object UpdateTicketsFailed : Inner
            data class UpdateTicketsCompleted(val tickets: TicketsInfo) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            class ShowFilterMenu(
                val appId: String,
                val selectedUserId: String
            ) : Outer

            class ShowAddTicketMenu(val appId: String) : Outer

            class ShowTicket(val ticketId: Int?, val userId: String? = null) : Outer
        }

        sealed interface Inner : Effect {
            data object UpdateTickets : Inner
        }
    }

}