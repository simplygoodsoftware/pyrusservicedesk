package com.pyrus.pyrusservicedesk.sdk

import java.util.concurrent.atomic.AtomicBoolean

class SingleEvent<out T>(private val content: T)  {
    private var hasBeenHandled = AtomicBoolean(false)

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled.getAndSet(true)) {
            null
        } else {
            content
        }
    }
}