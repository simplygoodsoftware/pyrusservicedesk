package com.pyrus.pyrusservicedesk._ref.helpers

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ThreadsHelper {

    /**
     *  Performs an action in the main thread, waiting for the result.
     *  Caution: Blocks the current thread. Do not perform complex or resource-intensive actions.
     *  @param action - action to be performed
     */
    fun syncRunOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            runBlocking {
                withContext(Dispatchers.Main) {
                    action()
                }
            }
        }
    }
}