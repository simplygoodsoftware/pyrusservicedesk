package com.pyrus.pyrusservicedesk.core

import android.util.Log
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog

/**
 * The SDK UI graph ([UiInjector]) shared by every SDK activity in the same task
 * (MainActivity + FilePreviewActivity), owned as a ref-counted resource.
 *
 * The graph can not be a plain single-activity scope: the SDK spans more than one activity, and
 * under "Don't keep activities" the backgrounded MainActivity is destroyed while FilePreviewActivity
 * — which shares the graph's Picasso and SharedViewModel — is still on screen. So the graph must
 * outlive any single activity. It also can not be built in `start()` (that would put heavy,
 * main-thread work on the launch path — the ANR this code already fixes), so it is built lazily by
 * whichever activity's [UiGraphViewModel] acquires it first.
 *
 * Contract (all calls on the main thread, driven by [UiGraphViewModel] init / onCleared):
 *  - [acquire] builds the graph on the first owner and reuses it for every later owner;
 *  - [release] closes and detaches the graph only when the last owner is released.
 *
 * This is what keeps the graph alive across MainActivity's destruction and subsumes the 1.8.13
 * close/reopen overlap protection — a finishing owner can not tear down a graph another owner holds.
 */
internal class SharedUiGraph(private val build: () -> UiInjector) {

    @Volatile
    var instance: UiInjector? = null
        private set

    private var ownerCountInternal = 0

    /** Number of live owners currently retaining the graph. */
    val ownerCount: Int
        get() = ownerCountInternal

    @MainThread
    fun acquire(): UiInjector {
        val graph = instance ?: build().also {
            instance = it
            log("built a new UI graph #${id(it)}")
        }
        ownerCountInternal++
        log("acquire: graph #${id(graph)}, owners=$ownerCountInternal")
        return graph
    }

    @MainThread
    fun release() {
        if (ownerCountInternal > 0) ownerCountInternal--
        log("release: graph #${id(instance)}, owners=$ownerCountInternal")
        if (ownerCountInternal == 0) {
            val graph = instance
            instance = null
            graph?.close()
        }
    }

    /** Test-only: drop the graph and reset the owner count without going through the lifecycle. */
    fun resetForTest() {
        instance = null
        ownerCountInternal = 0
    }

    private fun id(graph: UiInjector?): Int = System.identityHashCode(graph)

    private fun log(message: String) {
        Log.d(TAG, message)
        PLog.d(TAG, message)
    }

    private companion object {
        private const val TAG = "SharedUiGraph"
    }
}
