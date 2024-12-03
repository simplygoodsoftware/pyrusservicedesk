package com.pyrus.pyrusservicedesk.log

import android.app.Application
import androidx.core.util.Consumer
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.core.StaticRepository
import java.io.File

internal object PLog {

    @JvmField
    internal var initialized = false

    fun i(TAG: String?, msg: String?, vararg params: Any?) {
        if (StaticRepository.logging)
            _L.i(TAG, msg, *params)
    }

    fun d(TAG: String?, msg: String?, vararg params: Any?) {
        if (StaticRepository.logging)
            _L.d(TAG, msg, *params)
    }

    fun w(TAG: String?, msg: String?, vararg params: Any?) {
        if (StaticRepository.logging)
            _L.w(TAG, msg, *params)
    }

    fun e(TAG: String?, msg: String?, vararg params: Any?) {
        if (StaticRepository.logging)
            _L.e(TAG, msg, *params)
    }

    fun addLogSubscriber(subscriber: Consumer<File>) {
        _L.addLogSubscriber(subscriber)
    }

    fun removeLogSubscriber(subscriber: Consumer<File>) {
        _L.removeLogSubscriber(subscriber)
    }

    fun collectLogs() {
        _L.sendLogs()
    }


    fun instantiate(application: Application) {
        _L.Instantiate(application)
        initialized = true
    }

}