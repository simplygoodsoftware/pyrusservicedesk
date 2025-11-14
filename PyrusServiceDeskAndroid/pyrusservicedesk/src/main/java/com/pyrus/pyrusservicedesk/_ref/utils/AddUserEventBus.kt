package com.pyrus.pyrusservicedesk._ref.utils

import com.pyrus.pyrusservicedesk.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddUserEventBus {

    private val mutableSharedFlow = MutableSharedFlow<Unit>(replay = 1)

    private val pendingFilter = AtomicReference<User?>(null)

    suspend fun setFilter(filter: User) {
        pendingFilter.set(filter)
        mutableSharedFlow.emit(Unit)
    }

    fun getAndRemovePendingFilter(): User? {
        return pendingFilter.getAndSet(null)
    }

    fun events(): Flow<Unit> {
        return mutableSharedFlow.onEach { mutableSharedFlow.resetReplayCache() }
    }
}