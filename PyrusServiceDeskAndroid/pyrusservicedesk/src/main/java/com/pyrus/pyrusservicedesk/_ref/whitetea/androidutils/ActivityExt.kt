package com.pyrus.pyrusservicedesk._ref.whitetea.androidutils

import androidx.activity.ComponentActivity
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper.instanceKeeper

internal inline fun <reified T : Store<*, *, *>> ComponentActivity.getStore(key: Any, noinline factory: () -> T): T =
    instanceKeeper().getStore(key = key, factory = factory)