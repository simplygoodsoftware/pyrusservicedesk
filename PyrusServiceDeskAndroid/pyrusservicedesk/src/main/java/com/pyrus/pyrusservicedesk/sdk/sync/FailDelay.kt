package com.pyrus.pyrusservicedesk.sdk.sync

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.random.Random

internal class FailDelay {
    private val previousDelay = AtomicLong(0L)
    private val isCanceled = AtomicBoolean(false)

    /**
     * returns delay in milliseconds
     */
    private fun getNextDelay(): Long {
        var delay =
            if (previousDelay.get() == 0L)
                BASE_DELAY
            else min(previousDelay.get() * 3, MAX_DELAY)

        previousDelay.set(delay)
        delay = Random.nextLong(BASE_DELAY, delay + 1)
        return delay
    }

    fun clear() {
        previousDelay.set(0)
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

    companion object {

        private const val BASE_DELAY = 1000L
        private const val MAX_DELAY = 3 * 60 * 1000L
    }

}