package net.papirus.pyrusservicedesk.repository

import android.arch.lifecycle.LiveData
import android.net.Uri
import net.papirus.pyrusservicedesk.repository.data.Attachment
import net.papirus.pyrusservicedesk.repository.data.TicketDescription
import net.papirus.pyrusservicedesk.repository.updates.GetTicketUpdate
import net.papirus.pyrusservicedesk.repository.updates.GetTicketsUpdate
import net.papirus.pyrusservicedesk.repository.updates.UpdateSubscriber

internal interface Repository{
    fun subscribeToUpdates(subscriber: UpdateSubscriber)
    fun unsubscribeFromUpdates(subscriber: UpdateSubscriber)
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