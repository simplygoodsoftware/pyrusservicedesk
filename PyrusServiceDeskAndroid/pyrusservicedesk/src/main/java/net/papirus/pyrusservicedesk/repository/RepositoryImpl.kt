package net.papirus.pyrusservicedesk.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns.DISPLAY_NAME
import android.support.v4.content.ContentResolverCompat
import net.papirus.pyrusservicedesk.repository.data.Attachment
import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.updates.*
import net.papirus.pyrusservicedesk.repository.web_service.WebService
import net.papirus.pyrusservicedesk.repository.web_service.response.Status
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.*

internal class RepositoryImpl(
        private val webService: WebService,
        private val contentResolver: ContentResolver)
    : Repository {

    private val subscribers = HashMap<UpdateType, MutableSet<UpdateSubscriber>>()

    override fun subscribeToUpdates(subscriber: UpdateSubscriber) {
        subscriber.getUpdateTypes().forEach {
            var set = subscribers[it]
            if (set == null) {
                set = HashSet()
                subscribers[it] = set
            }
            set.add(subscriber)
        }

    }

    override fun unsubscribeFromUpdates(subscriber: UpdateSubscriber) {
        subscriber.getUpdateTypes().forEach{
            subscribers[it]?.remove(subscriber)
        }
    }

    override fun getTickets(): LiveData<GetTicketsUpdate> {
        return Transformations.map(webService.getTickets(GetTicketsRequest())){ response ->
            when {
                response?.responseData == null ->
                    GetTicketsUpdate(error = UpdateError.Unknown)

                response.status == Status.WebServiceError ->
                    GetTicketsUpdate(error = UpdateError.WebService)

                else ->
                    GetTicketsUpdate(response.responseData)
            }
        }
    }

    override fun getTicket(ticketId: Int): LiveData<GetTicketUpdate> {
        return Transformations.map(webService.getTicket(GetTicketRequest(ticketId))){ response ->
            when {
                response?.responseData == null ->
                    GetTicketUpdate(error = UpdateError.Unknown)

                response.status == Status.WebServiceError ->
                    GetTicketUpdate(error = UpdateError.WebService)

                else ->
                    GetTicketUpdate(response.responseData)
            }
        }
    }

    override fun addComment(ticketId: Int,
                            userName: String,
                            comment: String,
                            attachments: List<Attachment>?) {

        webService.addComment(AddCommentRequest(ticketId, userName, comment, attachments))
                .observeForever { response ->
                    notifySubscribers(
                            when{
                                response?.responseData == null ->
                                    CommentAddedUpdate(error = UpdateError.Unknown)
                                response.status == Status.WebServiceError ->
                                    CommentAddedUpdate(error = UpdateError.WebService)
                                else ->
                                    // TODO comment with new id should be passed to constructor arguments
                                    CommentAddedUpdate(response.request.ticketId)
                            }
                    )
                }
    }

    override fun createTicket(ticket: Ticket) {
        webService.createTicket(CreateTicketRequest(ticket))
                .observeForever {response ->
                    notifySubscribers(
                            when {
                                response?.responseData == null ->
                                    TicketCreatedUpdate(error = UpdateError.Unknown)
                                response.status == Status.WebServiceError ->
                                    TicketCreatedUpdate(error = UpdateError.WebService)
                                else ->
                                    TicketCreatedUpdate(response.responseData)
                            }
                    )
                }
    }

    override fun uploadFile(ticketId: Int, fileUri: Uri){
        val cursor = ContentResolverCompat.query(
                contentResolver,
                fileUri,
                null,
                null,
                null,
                null,
                null)

        if (!cursor.moveToFirst())
            return

        webService.uploadFile(
                UploadFileRequest(
                        ticketId,
                        cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)),
                        contentResolver.openInputStream(fileUri)))
                .observeForever { response ->
                    when {
                        response?.responseData == null ->
                            notifySubscribers(CommentAddedUpdate(error = UpdateError.Unknown))
                        response.status == Status.WebServiceError ->
                            notifySubscribers(CommentAddedUpdate(error = UpdateError.WebService))
                        else ->
                            addComment(
                                    ticketId,
                                    "USER",
                                    "",
                                    listOf(Attachment(response.responseData.guid)))
                    }
                }
    }


    private fun notifySubscribers(update: UpdateBase) {
        subscribers[update.type]?.forEach {
            it.onUpdateReceived(update)
        }
    }
}