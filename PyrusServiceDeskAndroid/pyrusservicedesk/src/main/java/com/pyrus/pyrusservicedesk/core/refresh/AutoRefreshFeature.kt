package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshContract.StartUpdates

typealias AutoRefreshFeature = Store<Unit, Unit, StartUpdates>

interface AutoRefreshContract {

    object StartUpdates

}