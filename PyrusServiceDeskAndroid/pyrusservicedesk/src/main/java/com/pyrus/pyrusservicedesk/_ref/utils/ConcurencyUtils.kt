package com.pyrus.pyrusservicedesk._ref.utils

import java.util.concurrent.BlockingQueue

internal fun <T> BlockingQueue<T>.drain(maxElements: Int): List<T> {
    val res = ArrayList<T>()
    drainTo(res, maxElements)
    return res
}