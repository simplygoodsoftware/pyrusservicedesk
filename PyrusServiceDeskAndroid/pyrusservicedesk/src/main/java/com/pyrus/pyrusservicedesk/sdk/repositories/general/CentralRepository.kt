package com.pyrus.pyrusservicedesk.sdk.repositories.general

import com.pyrus.pyrusservicedesk.sdk.data.Command
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.offline.OfflineRepository
import com.pyrus.pyrusservicedesk.sdk.response.GetTicketResponse
import com.pyrus.pyrusservicedesk.sdk.response.GetTicketsResponse
import com.pyrus.pyrusservicedesk.sdk.response.Response
import com.pyrus.pyrusservicedesk.sdk.response.SetPushTokenResponse

/**
 * [GeneralRepository] implementation that handles all general requests.
 */
internal class CentralRepository(private val webRepository: RemoteRepository,
                                 private val offlineRepository: OfflineRepository)
    : GeneralRepository {

    override suspend fun getFeed(keepUnread: Boolean): Response<Comments> =
        webRepository.getFeed(keepUnread)

    override suspend fun getTickets(commands: List<Command>): GetTicketsResponse = webRepository.getTickets(commands)

    override suspend fun getTicket(ticketId: Int): GetTicketResponse = webRepository.getTicket(ticketId)

    override suspend fun setPushToken(token: String?, tokenType: String): SetPushTokenResponse =
        webRepository.setPushToken(token, tokenType)

    override suspend fun addPendingFeedComment(comment: Comment) = offlineRepository.addPendingFeedComment(comment)

    override suspend fun getPendingFeedComments() = offlineRepository.getPendingFeedComments()

    override suspend fun removePendingComment(comment: Comment) = offlineRepository.removePendingComment(comment)

    override suspend fun removeAllPendingComments() = offlineRepository.removeAllPendingComments()

}
