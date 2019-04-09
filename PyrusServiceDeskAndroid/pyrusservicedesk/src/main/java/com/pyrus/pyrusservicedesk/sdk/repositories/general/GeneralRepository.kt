package com.pyrus.pyrusservicedesk.sdk.repositories.general

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks

/**
 * Interface for the objects that are responsible for handling general user requests requests.
 * TODO the better approach is to split it to separate repos. At leas [setPushToken] is not general use case.
 */
internal interface GeneralRepository{
    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(): GetFeedResponse

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
    suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks? = null): AddCommentResponse

    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks? = null): AddCommentResponse

    /**
     * Creates ticket using the given [description].
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun createTicket(description: TicketDescription, uploadFileHooks: UploadFileHooks? = null): CreateTicketResponse

    /**
     * Registers the given push [token].
     */
    suspend fun setPushToken(token: String): SetPushTokenResponse
}