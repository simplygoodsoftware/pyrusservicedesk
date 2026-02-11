package com.pyrus.pyrusservicedesk.sdk.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SystemMessageStore(
    private val idStore: IdStore
) {

    private val ticketState: MutableStateFlow<Long?> = MutableStateFlow(null)
    fun ticketStateFlow(): StateFlow<Long?> = ticketState

    fun ticketId(): Long? = ticketState.value

    private val operatorResponseTimeMessageState: MutableStateFlow<String?> = MutableStateFlow(null)
    fun operatorResponseTimeMessageStateFlow(): StateFlow<String?> = operatorResponseTimeMessageState

    fun setNecessityTimeSystemMessage(ticketId: Long, isNecessary: Boolean) {
        val id = idStore.getTicketServerId(ticketId) ?: ticketId
        if (isNecessary)
            ticketState.value = id
        else {
            ticketState.value = null
            operatorResponseTimeMessageState.value = null
        }
    }

    fun setOperatorResponseTimeMessage(ticketId: Long, message: String?) {
        if (message.isNullOrBlank()) {
            setNecessityTimeSystemMessage(ticketId, false)
            return
        }
        setNecessityTimeSystemMessage(ticketId, true)
        operatorResponseTimeMessageState.value = message
    }
}