package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import androidx.fragment.app.FragmentManager
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.core.Account

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
            val tickets: List<TicketSetInfo>?,
        ) : ContentState
    }

    sealed interface Message {

        sealed interface Outer : Message {
            data class OnFilterClick(val selectedUserId: String) : Outer
            data object OnScanClick : Outer
            data object OnSettingsClick : Outer
            data object OnFabItemClick : Outer
            data class OnChangePage(val appId: String) : Outer
            data object OnCreateTicketClick : Outer

            // TODO почему userId нулабелен
            data class OnTicketClick(val ticketId: Int, val userId: String?) : Outer

            // TODO если не нужно – удалить
            data class OnUserIdSelect(val userId: String) : Outer
        }

        sealed interface Inner : Message {
            data class TicketsUpdated(val tickets: TicketsInfo?) : Inner
            data class UserIdSelected(val userId: String, val fm: FragmentManager) : Inner
            data object UpdateTicketsFailed : Inner
            data class UpdateTicketsCompleted(val tickets: TicketsInfo) : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {
            data class ShowFilterMenu(
                val appId: String,
                val selectedUserId: String
            ) : Outer

            data class ShowAddTicketMenu(val appId: String) : Outer

            data object OpenQrFragment : Outer

            data object OpenSettingsFragment : Outer

        }

        sealed interface Inner : Effect {
            data object TicketsSetFlow : Inner
            data class UpdateTickets(val force: Boolean) : Inner
            data class OpenTicketScreen(val appId: String, val ticketId: Int?, val userId: String?) : Inner
        }
    }

}