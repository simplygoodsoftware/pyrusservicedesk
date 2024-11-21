package com.pyrus.pyrusservicedesk._ref.whitetea.utils

import androidx.lifecycle.Lifecycle
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.subscribe
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.Binder
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode

/**
 * Attaches the [Binder] to the provided [Lifecycle] using the [BinderLifecycleMode].
 * Depending on the [BinderLifecycleMode] will subscribe to appropriate [Lifecycle] callbacks
 * and will call corresponding [Binder.start] and [Binder.stop] methods when notified.
 *
 * @receiver a [Binder] to be controlled by the [Lifecycle]
 * @param lifecycle a [Lifecycle] that will provide lifecycle events
 * @param mode a [BinderLifecycleMode] to be used when subscribing for lifecycle events
 */
internal fun Binder.attachTo(lifecycle: Lifecycle, mode: BinderLifecycleMode): Binder =
    when (mode) {
        BinderLifecycleMode.CREATE_DESTROY -> lifecycle.subscribe(onCreate = ::start, onDestroy = ::stop)
        BinderLifecycleMode.START_STOP -> lifecycle.subscribe(onStart = ::start, onStop = ::stop)
        BinderLifecycleMode.RESUME_PAUSE -> lifecycle.subscribe(onResume = ::start, onPause = ::stop)
    }.let { this }