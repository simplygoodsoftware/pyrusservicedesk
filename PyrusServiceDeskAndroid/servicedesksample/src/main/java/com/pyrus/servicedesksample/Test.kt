package com.pyrus.servicedesksample

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Test {

    @JvmStatic
    fun stopSd() {
        GlobalScope.launch {
            delay(2000)
            withContext(Dispatchers.Main) {
                PyrusServiceDesk.stop()
            }
        }
    }

    @JvmStatic
    fun refreshSd() {
        GlobalScope.launch {
            delay(2000)
            withContext(Dispatchers.Main) {
                PyrusServiceDesk.refresh()
            }
        }
    }

}