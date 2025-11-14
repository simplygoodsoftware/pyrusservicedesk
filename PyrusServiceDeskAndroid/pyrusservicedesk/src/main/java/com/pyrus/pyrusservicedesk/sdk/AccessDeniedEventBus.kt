package com.pyrus.pyrusservicedesk.sdk

import com.pyrus.pyrusservicedesk.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class AccessDeniedEventBus {

    private val mutableSharedFlow = MutableSharedFlow<List<User>>()

    suspend fun post(event: List<User>) {
        mutableSharedFlow.emit(event)
    }

    fun events(): Flow<List<User>> {
        return mutableSharedFlow
    }
}