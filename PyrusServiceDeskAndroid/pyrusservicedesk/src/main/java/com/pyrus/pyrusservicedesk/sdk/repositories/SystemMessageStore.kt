package com.pyrus.pyrusservicedesk.sdk.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SystemMessageStore(
    private val idStore: IdStore
) {

    private val ticketState: MutableStateFlow<Long?> = MutableStateFlow(null)
    fun ticketStateFlow(): StateFlow<Long?> = ticketState

    private val operatorResponseTimeMessageState: MutableStateFlow<String?> = MutableStateFlow(null)
    fun operatorResponseTimeMessageStateFlow(): StateFlow<String?> = operatorResponseTimeMessageState

    fun setNecessityTimeSystemMessage(ticketId: Long, isNecessary: Boolean) {
        val id = idStore.getTicketServerId(ticketId) ?: ticketId
        Log.d("EP ", "ticketId: $id, isNecessary: $isNecessary")
        //val newMessages = operatorResponseTimeMessageState.value?.toMutableMap()
        if (isNecessary)
            ticketState.value = id
        else {
            ticketState.value = null
            operatorResponseTimeMessageState.value = null
        }
    }

    fun setOperatorResponseTimeMessage(ticketId: Long, message: String?) {
        if (message == null) {
            setNecessityTimeSystemMessage(ticketId, false)
            return
        }
        operatorResponseTimeMessageState.value = message
    }
}