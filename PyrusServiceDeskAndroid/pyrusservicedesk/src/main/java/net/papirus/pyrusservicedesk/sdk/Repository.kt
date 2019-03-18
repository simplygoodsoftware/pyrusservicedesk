package net.papirus.pyrusservicedesk.sdk

import android.arch.lifecycle.LiveData
import android.net.Uri
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.updates.GetTicketFeedUpdate
import net.papirus.pyrusservicedesk.sdk.updates.GetTicketUpdate
import net.papirus.pyrusservicedesk.sdk.updates.GetTicketsUpdate
import net.papirus.pyrusservicedesk.sdk.updates.UpdateSubscriber

internal interface Repository{
    fun subscribeToUpdates(subscriber: UpdateSubscriber)
    fun unsubscribeFromUpdates(subscriber: UpdateSubscriber)
    fun getTicketFeed(): LiveData<GetTicketFeedUpdate>
    fun getTickets(): LiveData<GetTicketsUpdate>
    fun getTicket(ticketId: Int): LiveData<GetTicketUpdate>
    fun createTicket(userName: String, ticket: TicketDescription)
    // TODO Comment should be passed, must handle file uploading too
    fun addComment(
        ticketId: Int,
        comment: String,
        attachments: List<Attachment>? = null
    )
    // TODO temporary approach, addComment must be sufficient
    fun retryComment(ticketId: Int, localComment: Comment)
    fun uploadFile(ticketId: Int, fileUri: Uri)
}