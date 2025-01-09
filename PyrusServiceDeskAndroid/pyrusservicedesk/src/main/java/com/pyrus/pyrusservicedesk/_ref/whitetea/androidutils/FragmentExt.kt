package com.pyrus.pyrusservicedesk._ref.whitetea.androidutils

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.Binder
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BindingsBuilder
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper.instanceKeeper
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.bind as binder

internal fun Fragment.bind(
    mode: BinderLifecycleMode = BinderLifecycleMode.START_STOP,
    mainContext: CoroutineContext = Dispatchers.Main,
    builder: BindingsBuilder.() -> Unit
): Binder {

    return binder(lifecycleScope, lifecycle, mode, mainContext, builder)
}

internal inline fun <reified T : Store<*, *, *>> Fragment.getStore(noinline factory: () -> T): T =
    instanceKeeper().getStore(key = T::class, factory = factory)