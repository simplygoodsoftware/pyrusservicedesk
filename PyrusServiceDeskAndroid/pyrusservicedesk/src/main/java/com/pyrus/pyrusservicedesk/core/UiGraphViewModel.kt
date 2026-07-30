package com.pyrus.pyrusservicedesk.core

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog

/**
 * One owner of the shared SDK UI graph ([UiInjector]).
 *
 * Scoped to an SDK activity's [androidx.lifecycle.ViewModelStore]. The graph itself is ref-counted
 * in [PyrusServiceDesk] and shared across every SDK activity in the task (MainActivity +
 * FilePreviewActivity), so it:
 *  - is built on the first owner's [PyrusServiceDesk.acquireUiInjector] and reused by later owners;
 *  - survives configuration changes for free (the ViewModel is retained, so no acquire/release runs);
 *  - survives one activity's destruction while another SDK activity is still alive — e.g. "Don't keep
 *    activities" destroying the backgrounded MainActivity while FilePreviewActivity is on screen;
 *  - is closed exactly once, when the last owner is released in [onCleared]
 *  — no manual isFinishing bookkeeping required.
 */
internal class UiGraphViewModel : ViewModel() {

    init {
        Log.d(TAG, "init (owner #${System.identityHashCode(this)}): acquiring UI graph")
        PLog.d(TAG, "init (owner #${System.identityHashCode(this)}): acquiring UI graph")
        PyrusServiceDesk.acquireUiInjector()
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared (owner #${System.identityHashCode(this)}): releasing UI graph")
        PLog.d(TAG, "onCleared (owner #${System.identityHashCode(this)}): releasing UI graph")
        PyrusServiceDesk.releaseUiInjector()
        super.onCleared()
    }

    private companion object {
        private const val TAG = "UiGraphViewModel"
    }
}
