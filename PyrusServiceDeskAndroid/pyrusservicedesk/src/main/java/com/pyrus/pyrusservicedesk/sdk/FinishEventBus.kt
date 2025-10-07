package com.pyrus.pyrusservicedesk.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FinishEventBus {

    private val mutableSharedFlow = MutableStateFlow(false)

    fun post(event: Boolean) {
        mutableSharedFlow.value = event
    }

    fun events(): Flow<Boolean> {
        return mutableSharedFlow
    }
}