package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store

internal typealias TicketFeature = Store<TicketContract.State, TicketContract.Message, TicketContract.Effect>

internal interface TicketContract {

    class State

    sealed interface Message {

        sealed interface Outer : Message {

            object OnPreviewClick : Outer

            object OnRetryClick : Outer

            data class OnCopyClick(val text: String) : Outer

            data class OnRatingClick(val rating: Int) : Outer

            object OnShowAttachVariantsClick : Outer

            object OnSendClick : Outer

            object OnCloseClick : Outer

            data class OnMessageChanged(val text: String) : Outer

            data class OnAttachmentSelected(val fileUri: Uri?) : Outer

        }

        sealed interface Inner : Message

    }

    sealed interface Effect {
        sealed interface Outer : Effect
        sealed interface Inner : Effect
    }

}