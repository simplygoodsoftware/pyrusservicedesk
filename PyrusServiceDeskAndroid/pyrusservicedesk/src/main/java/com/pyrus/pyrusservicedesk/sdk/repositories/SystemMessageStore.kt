package com.pyrus.pyrusservicedesk.sdk.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SystemMessageStore() {

    private val ticketState: MutableStateFlow<Long?> = MutableStateFlow(null)
    fun ticketStateFlow(): StateFlow<Long?> = ticketState

    private val operatorResponseTimeMessageState: MutableStateFlow<Map<Long, String?>?> = MutableStateFlow(null)
    fun operatorResponseTimeMessageStateFlow(): StateFlow<Map<Long, String?>?> = operatorResponseTimeMessageState

    fun setNecessityTimeSystemMessage(ticketId: Long, isNecessary: Boolean) {
        Log.d("EP ", "ticketId: $ticketId, isNecessary: $isNecessary")
        //val list = ticketState.value?.toMutableList() ?: mutableListOf()
        val newMessages = operatorResponseTimeMessageState.value?.toMutableMap()
        if (isNecessary)
            ticketState.value = ticketId
            //list.add(ticketId)
        else {
            ticketState.value = null
            //list.remove(ticketId)
            newMessages?.remove(ticketId)
        }
        //ticketState.value = list
        operatorResponseTimeMessageState.value = newMessages
    }

    fun setOperatorResponseTimeMessage(ticketId: Long, message: String?) {
        if (message == null) {
            setNecessityTimeSystemMessage(ticketId, false)
            return
        }
        val newMessages = operatorResponseTimeMessageState.value?.toMutableMap()
        newMessages?.set(ticketId, message)
        operatorResponseTimeMessageState.value = newMessages
    }
}