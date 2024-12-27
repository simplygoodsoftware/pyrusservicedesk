package com.pyrus.pyrusservicedesk.sdk.sync

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

internal class FailDelayCounter {

    private val attemptCount = AtomicInteger(0)

    /**
     * returns delay in milliseconds
     */
    fun getNextDelay(): Long {
        val pov = attemptCount.incrementAndGet().toDouble()
        if (pov > 8) return 2.0.pow(8).toLong() * 1000
        return 2.0.pow(pov).toLong() * 1000
    }

    fun clear() {
        attemptCount.set(0)
    }

}