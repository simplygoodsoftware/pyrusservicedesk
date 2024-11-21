package com.pyrus.pyrusservicedesk.sdk.repositories.general

import com.pyrus.pyrusservicedesk.sdk.data.Command
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.response.*

internal interface RemoteRepository {
    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean): Response<Comments>

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(commands: List<Command>): GetTicketsResponse

    /**
     * Provides ticket with the given [ticketId].
     */
    suspend fun getTicket(ticketId: Int): GetTicketResponse

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(token: String?, tokenType: String): SetPushTokenResponse
}