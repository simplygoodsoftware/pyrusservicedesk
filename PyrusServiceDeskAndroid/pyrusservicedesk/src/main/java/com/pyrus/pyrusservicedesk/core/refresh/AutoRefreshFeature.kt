package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store

typealias AutoRefreshFeature = Store<Unit, Unit, AutoRefreshContract.Effect>

interface AutoRefreshContract {

    sealed interface Effect {
        object StartUpdates : Effect
        object StartUpdatesSystemMessage : Effect
    }

}