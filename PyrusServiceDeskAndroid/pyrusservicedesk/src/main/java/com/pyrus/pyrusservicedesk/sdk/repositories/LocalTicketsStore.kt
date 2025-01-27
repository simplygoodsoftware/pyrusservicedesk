package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class LocalTicketsStore(
    private val idStore: IdStore,
) {

    private val ticketsInfoState = MutableStateFlow<TicketsDto?>(null)

    fun getTickets(): TicketsDto? {
        return ticketsInfoState.value
    }

    fun getTicket(ticketId: Long): TicketDto? {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        if (serverTicketId <= 0) return null
        return ticketsInfoState.value?.tickets?.find { it.ticketId == serverTicketId }
    }

    fun applyDiff(tickets: TicketsDto): TicketsDto {
        val localState = getTickets()
        val mergedState = mergeDiff(localState, tickets)
        ticketsInfoState.value = mergedState
        return mergedState
    }

    fun getTicketInfoFlow(): Flow<TicketsDto?> = ticketsInfoState

    fun getTicketInfoFlow(ticketId: Long): Flow<TicketDto?> = ticketsInfoState.map { ticketsDto ->
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        if (serverTicketId <= 0) return@map null
        ticketsDto?.tickets?.find { it.ticketId == serverTicketId }
    }

    private fun mergeDiff(localState: TicketsDto?, diff: TicketsDto): TicketsDto {
        if (localState == null) return diff

        val ticketsDiff = diff.tickets ?: emptyList()
        val ticketsMap = localState.tickets?.associateBy { it.ticketId } ?: emptyMap()

        val mergedTickets = ArrayList<TicketDto>()

        for (ticket in ticketsDiff) {
            val localTicket = ticketsMap[ticket.ticketId]
            if (localTicket == null) {
                mergedTickets.add(ticket)
                continue
            }
            val localComments = localTicket.comments
            val commentsDiff = ticket.comments
            val mergedComments: List<CommentDto>? = when {
                localComments != null && commentsDiff != null -> {
                    val commentsMap = localComments.associateBy { it.commentId }.toMutableMap()
                    commentsMap.putAll(commentsDiff.associateBy { it.commentId })
                    commentsMap.values.toList()
                }
                localComments != null -> localComments
                commentsDiff != null -> commentsDiff
                else -> null
            }
            val mergedTicket = ticket.copy(comments = mergedComments)
            mergedTickets += mergedTicket
        }

        return TicketsDto(
            hasMore = false,
            applications = diff.applications,
            tickets = mergedTickets,
            commandsResult = null,
            authorAccessDenied = diff.authorAccessDenied,
        )
    }

}