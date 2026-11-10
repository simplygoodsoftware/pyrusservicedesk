package com.pyrus.pyrusservicedesk._ref

interface TimeProvider {
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}

class PsdTimeProvider: TimeProvider {
    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}