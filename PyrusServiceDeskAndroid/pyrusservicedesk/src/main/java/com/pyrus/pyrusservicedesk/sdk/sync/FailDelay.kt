package com.pyrus.pyrusservicedesk.sdk.sync

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

internal class FailDelay {

    private val attemptCount = AtomicInteger(0)
    private val isCanceled = AtomicBoolean(false)

    /**
     * returns delay in milliseconds
     */
    private fun getNextDelay(): Long {
        val pov = attemptCount.incrementAndGet().toDouble()
        if (pov > 8) return 2.0.pow(8).toLong() * 1000
        return 2.0.pow(pov).toLong() * 1000
    }

    fun clear() {
        attemptCount.set(0)
    }

    fun cancel() {
        isCanceled.set(true)
    }

    suspend fun cancelableDelay() {
        isCanceled.set(false)
        val delay = getNextDelay()
        val delayPart = delay / 1000
        for (i in 0 until delayPart) {
            if (isCanceled.get()) break
            delay(1000L)
        }
    }

}