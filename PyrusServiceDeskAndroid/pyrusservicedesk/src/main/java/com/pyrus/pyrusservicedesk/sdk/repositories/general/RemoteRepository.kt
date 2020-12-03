package com.pyrus.pyrusservicedesk.sdk.repositories.general

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

internal interface RemoteRepository {
    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(): Response<Comments>

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(): GetTicketsResponse

    /**
     * Provides ticket with the given [ticketId].
     */
    suspend fun getTicket(ticketId: Int): GetTicketResponse

    /**
     * Appends [comment] to the ticket with the given [ticketId].
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks? = null): Response<AddCommentResponseData>

    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks? = null): Response<AddCommentResponseData>

    /**
     * Creates ticket using the given [description].
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun createTicket(description: TicketDescription, uploadFileHooks: UploadFileHooks? = null): CreateTicketResponse

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     */
    suspend fun setPushToken(token: String?): SetPushTokenResponse
}