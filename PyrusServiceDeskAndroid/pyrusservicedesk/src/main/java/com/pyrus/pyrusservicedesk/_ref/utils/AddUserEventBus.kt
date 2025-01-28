package com.pyrus.pyrusservicedesk._ref.utils

import com.pyrus.pyrusservicedesk.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach

internal class AddUserEventBus {

    private val mutableSharedFlow = MutableSharedFlow<User>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun post(event: User) {
        mutableSharedFlow.emit(event)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun events(): Flow<User> {
        return mutableSharedFlow.onEach { mutableSharedFlow.resetReplayCache() }
    }
}