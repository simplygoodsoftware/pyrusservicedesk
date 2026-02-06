package com.pyrus.pyrusservicedesk._ref.helpers

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ThreadsHelper {
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