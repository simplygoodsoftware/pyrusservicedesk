package net.papirus.pyrusservicedesk.sdk.repositories.general

import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal interface GeneralRepository{
    suspend fun getFeed(): GetFeedResponse
    suspend fun getTickets(): GetTicketsResponse
    suspend fun getTicket(ticketId: Int): GetTicketResponse
    suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks? = null): AddCommentResponse
    suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks? = null): AddCommentResponse
    suspend fun createTicket(description: TicketDescription, uploadFileHooks: UploadFileHooks? = null): CreateTicketResponse
    suspend fun setPushToken(token: String): SetPushTokenResponse
}