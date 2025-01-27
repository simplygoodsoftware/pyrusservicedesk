package com.pyrus.pyrusservicedesk._ref.utils
import com.pyrus.pyrusservicedesk.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class EventBus {
    private val channel = Channel<Pair<String, User>>(Channel.BUFFERED)

    suspend fun postEvent(event: Pair<String, User>) {
        channel.send(event)
    }

    fun events(): Flow<Pair<String, User>> {
        return channel.receiveAsFlow()
    }
}