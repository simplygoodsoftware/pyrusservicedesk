package net.papirus.pyrusservicedesk.sdk

import android.arch.lifecycle.LiveData
import android.net.Uri
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.updates.GetConversationUpdate
import net.papirus.pyrusservicedesk.sdk.updates.GetTicketUpdate
import net.papirus.pyrusservicedesk.sdk.updates.GetTicketsUpdate
import net.papirus.pyrusservicedesk.sdk.updates.UpdateSubscriber

internal interface Repository{
    fun subscribeToUpdates(subscriber: UpdateSubscriber)
    fun unsubscribeFromUpdates(subscriber: UpdateSubscriber)
    fun getConversation(): LiveData<GetConversationUpdate>
    fun getTickets(): LiveData<GetTicketsUpdate>
    fun getTicket(ticketId: Int): LiveData<GetTicketUpdate>
    fun createTicket(
        userName: String,
        ticket: TicketDescription
    )
    // TODO Comment should be passed
    fun addComment(
        ticketId: Int,
        comment: String,
        attachments: List<Attachment>? = null
    )
    fun uploadFile(ticketId: Int, fileUri: Uri)
}