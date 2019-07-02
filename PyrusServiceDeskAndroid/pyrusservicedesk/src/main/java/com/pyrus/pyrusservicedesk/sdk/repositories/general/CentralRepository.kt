package com.pyrus.pyrusservicedesk.sdk.repositories.general

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.offline.OfflineRepository
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * [GeneralRepository] implementation that handles all general requests.
 */
internal class CentralRepository(private val webRepository: RemoteRepository,
                                 private val offlineRepository: OfflineRepository)
    : GeneralRepository {

    override suspend fun getFeed(): Response<List<Comment>> = webRepository.getFeed()

    override suspend fun getTickets(): GetTicketsResponse = webRepository.getTickets()

    override suspend fun getTicket(ticketId: Int): GetTicketResponse = webRepository.getTicket(ticketId)

    override suspend fun addComment(ticketId: Int,
                                    comment: Comment,
                                    uploadFileHooks: UploadFileHooks?): Response<AddCommentResponseData>{


        return webRepository.addComment(ticketId, comment, uploadFileHooks)
    }

    override suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks?): Response<AddCommentResponseData> {
        addPendingFeedComment(comment)
        val response = webRepository.addFeedComment(comment, uploadFileHooks)
        if (!response.hasError()) {
            removePendingComment(comment)
        }
        return response
    }

    override suspend fun createTicket(description: TicketDescription,
                                      uploadFileHooks: UploadFileHooks?): CreateTicketResponse {

        return webRepository.createTicket(description, uploadFileHooks)
    }

    override suspend fun setPushToken(token: String): SetPushTokenResponse = webRepository.setPushToken(token)

    override suspend fun addPendingFeedComment(comment: Comment) = offlineRepository.addPendingFeedComment(comment)

    override suspend fun getPendingFeedComments() = offlineRepository.getPendingFeedComments()

    override suspend fun removePendingComment(comment: Comment) = offlineRepository.removePendingComment(comment)
}