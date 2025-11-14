package com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied

import android.content.Intent
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Message
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store

internal typealias AccessDeniedFeature = Store<Unit, Message, Effect.Outer>

interface AccessFeatureContract {

    sealed interface Message {


        sealed interface Outer : Message {
            data class OnDialogPositiveButtonClick(
                val goBack: Boolean,
            ): Outer

        }

        sealed interface Inner : Message {
            data class AccessDenied(
                val users: List<User>,
                val usersIsEmpty: Boolean,
            ) : Inner

            data object Finish : Inner
        }

    }

    sealed interface Effect {

        sealed interface Outer : Effect {

            data class ShowAccessDeniedDialog(
                val message: TextProvider,
                val usersIsEmpty: Boolean,
            ) : Outer

            data object CloseServiceDesk : Outer

            data class OpenBackwardScreen(val screen: Intent) : Outer
        }

        sealed interface Inner : Effect {
            data object AccessDeniedFlow : Inner
            data object CheckFinishFlow : Inner
        }
    }

}