package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine


internal class Repository(
    private val localStore: LocalStore,
    private val remoteStore: RemoteStore,
) {

    private val remoteTicketStateFlow: MutableStateFlow<Ticket?> = MutableStateFlow(null)

    fun getFeedFlow(): Flow<Ticket?> = combine(localStore.commentsFlow(), remoteTicketStateFlow) { local, remote ->
        when (remote) {
            null -> Ticket(
                comments = local,
                isRead = true, //TODO
                lastComment = null,
                isActive = null,
                createdAt = null,
            )
            else -> mergeComments(local, remote)
        }
    }

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean, ticketId: Int = 0, includePendingComments: Boolean = false): Try<Ticket?> {
        val dataTry = remoteStore.getAllData() // Feed(keepUnread)
        if (dataTry.isSuccess()) remoteTicketStateFlow.value = dataTry.value.tickets?.find { it.ticketId == ticketId }

        if (includePendingComments) {
            //return dataTry.map { mergeComments(localStore.getPendingFeedComments(), remoteTicketStateFlow) }
        }
        return when (dataTry) {
            is Try.Success -> Try.Success(remoteTicketStateFlow.value)
            is Try.Failure -> Try.Failure(dataTry.error)
        }
    }

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(): Try<List<Ticket>> {
        return remoteStore.getTickets()
    }

    suspend fun getAllData(): Try<Tickets> {
        return remoteStore.getAllData()
    }

    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addFeedComment(
        comment: Comment,
        uploadFileHooks: UploadFileHooks?
    ): Try<AddCommentResponseData> {
        localStore.addPendingFeedComment(comment)
        val response = remoteStore.addFeedComment(comment, uploadFileHooks)
        if (response.isSuccess()) {
            localStore.removePendingComment(comment)
        }
        return response
    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(token: String?, tokenType: String): Try<Unit> {
        return remoteStore.setPushToken(token, tokenType)
    }

    fun removePendingComment(comment: Comment) {
        return localStore.removePendingComment(comment)
    }


    private fun mergeComments(local: List<Comment>, remote: Ticket): Ticket {
        val comments = ArrayList<Comment>(local)
        if (remote.comments != null) {
            comments.addAll(remote.comments)
            comments.sortBy { it.creationDate.time }
        }
        return remote.copy(comments = comments)
    }
}