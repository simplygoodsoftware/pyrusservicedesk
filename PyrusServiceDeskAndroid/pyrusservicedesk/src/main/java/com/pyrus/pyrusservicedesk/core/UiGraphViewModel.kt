package com.pyrus.pyrusservicedesk.core

import androidx.lifecycle.ViewModel
import com.pyrus.pyrusservicedesk.PyrusServiceDesk

/**
 * Owns the lifetime of the SDK UI graph ([UiInjector]).
 *
 * Scoped to the SDK activity's [androidx.lifecycle.ViewModelStore], so the graph:
 *  - is created once, in the constructor, before the framework restores fragments;
 *  - survives configuration changes for free;
 *  - is closed exactly once, in [onCleared], when the activity is really finishing
 *  — no manual reference counting or isFinishing bookkeeping required.
 *
 */
internal class UiGraphViewModel : ViewModel() {

    private val uiInjector: UiInjector = PyrusServiceDesk.injector().createUiInjector()

    init {
        PyrusServiceDesk.publishUiInjector(uiInjector)
    }

    override fun onCleared() {
        PyrusServiceDesk.clearUiInjector(uiInjector)
        super.onCleared()
    }
}
