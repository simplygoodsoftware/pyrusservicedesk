package net.papirus.pyrusservicedesk.sdk

import kotlinx.coroutines.withContext
import net.papirus.pyrusservicedesk.PyrusServiceDesk.Companion.DISPATCHER_IO_SINGLE
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CentralRepository(private val webRepository: Repository) : Repository {

    override suspend fun getFeed(): GetConversationResponse = webRepository.getFeed()

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

        return withContext(DISPATCHER_IO_SINGLE){
            webRepository.createTicket(description, uploadFileHooks)
        }
    }
}