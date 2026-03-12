package com.pyrus.pyrusservicedesk.sdk.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class IdStore {

    private val ticketIdLock = ReentrantLock()
    private val commentIdLock = ReentrantLock()

    private val ticketIdStateFlow = MutableStateFlow(0)

    // <LocalId, ServerId>
    private val ticketLocalIdMap = HashMap<Long?, Long>()
    // <ServerId, LocalId>
    private val ticketServerIdMap = HashMap<Long, Long?>()

    // <LocalId, ServerId>
    private val commentsLocalIdMap = HashMap<Long, Long>()
    // <ServerId, LocalId>
    private val commentsServerIdMap = HashMap<Long, Long>()

    val ticketIdFlow = MutableStateFlow(0L)

    fun setTicketId(ticketId: Long) {
        ticketIdFlow.value = ticketId
    }

    fun addTicketIdPair(localId: Long?, serverId: Long) = ticketIdLock.withLock {
        ticketLocalIdMap[localId] = serverId
        ticketServerIdMap[serverId] = localId
        updateTicketIdTrigger()
    }

    fun getTicketServerId(localId: Long?): Long? = ticketIdLock.withLock {
        ticketLocalIdMap[localId]
    }

    fun getTicketServerIdFlow(localId: Long) = ticketIdStateFlow.map {
        getTicketServerId(localId) ?: localId
    }

    fun getTicketLocalId(serverId: Long): Long? = ticketIdLock.withLock {
        ticketServerIdMap[serverId]
    }

    fun addCommentIdPair(localId: Long, serverId: Long) = commentIdLock.withLock {
        commentsLocalIdMap[localId] = serverId
        commentsServerIdMap[serverId] = localId
    }

    fun getCommentServerId(localId: Long): Long? = ticketIdLock.withLock {
        return@withLock commentsLocalIdMap[localId]
    }

    fun getCommentLocalId(serverId: Long): Long? = ticketIdLock.withLock {
        return@withLock commentsServerIdMap[serverId]
    }

    private fun updateTicketIdTrigger() {
        ticketIdStateFlow.value += 1
    }
}