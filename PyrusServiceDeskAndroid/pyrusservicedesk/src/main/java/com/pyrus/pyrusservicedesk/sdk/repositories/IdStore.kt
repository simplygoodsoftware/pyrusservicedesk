package com.pyrus.pyrusservicedesk.sdk.repositories

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class IdStore {

    private val ticketIdLock = ReentrantLock();
    private val commentIdLock = ReentrantLock();

    // <LocalId, ServerId>
    private val ticketLocalIdMap = HashMap<Long, Long>()
    // <ServerId, LocalId>
    private val ticketServerIdMap = HashMap<Long, Long>()

    // <LocalId, ServerId>
    private val commentsLocalIdMap = HashMap<Long, Long>()
    // <ServerId, LocalId>
    private val commentsServerIdMap = HashMap<Long, Long>()

    fun addTicketIdPair(localId: Long, serverId: Long) = ticketIdLock.withLock {
        ticketLocalIdMap[localId] = serverId
        ticketServerIdMap[serverId] = localId
    }

    fun getTicketServerId(localId: Long): Long? = ticketIdLock.withLock {
        ticketLocalIdMap[localId]
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





}