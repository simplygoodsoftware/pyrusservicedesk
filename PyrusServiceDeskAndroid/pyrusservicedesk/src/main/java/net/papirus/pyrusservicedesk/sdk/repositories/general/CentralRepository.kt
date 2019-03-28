package net.papirus.pyrusservicedesk.sdk.repositories.general

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CentralRepository(private val webRepository: GeneralRepository) :
    GeneralRepository {

    override suspend fun getFeed(): GetFeedResponse = webRepository.getFeed()

    override suspend fun getTickets(): GetTicketsResponse = webRepository.getTickets()

    override suspend fun getTicket(ticketId: Int): GetTicketResponse = webRepository.getTicket(ticketId)

    override suspend fun addComment(ticketId: Int,
                                    comment: Comment,
                                    uploadFileHooks: UploadFileHooks?): AddCommentResponse{


        return webRepository.addComment(ticketId, comment, uploadFileHooks)
    }

    override suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks?): AddCommentResponse {
        return webRepository.addFeedComment(comment, uploadFileHooks)
    }

    override suspend fun createTicket(description: TicketDescription,
                                      uploadFileHooks: UploadFileHooks?): CreateTicketResponse {

        return webRepository.createTicket(description, uploadFileHooks)
    }

    override suspend fun setPushToken(token: String): SetPushTokenResponse = webRepository.setPushToken(token)
}