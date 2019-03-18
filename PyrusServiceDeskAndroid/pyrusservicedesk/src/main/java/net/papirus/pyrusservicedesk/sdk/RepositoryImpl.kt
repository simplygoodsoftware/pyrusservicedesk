package net.papirus.pyrusservicedesk.sdk

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns.DISPLAY_NAME
import android.provider.OpenableColumns.SIZE
import android.support.v4.content.ContentResolverCompat
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.LocalDataProvider
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.updates.*
import net.papirus.pyrusservicedesk.sdk.web_service.WebService
import net.papirus.pyrusservicedesk.sdk.web_service.response.Status
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.*

internal class RepositoryImpl(
        private val webService: WebService,
        private val contentResolver: ContentResolver,
        private val localDataProvider: LocalDataProvider)
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


    override fun getTicketFeed(): LiveData<GetTicketFeedUpdate> {
        return Transformations.map(webService.getTicketFeed(RequestBase())){ response ->
            when {
                response?.responseData == null ->
                    GetTicketFeedUpdate(error = UpdateError.Unknown)

                response.status == Status.WebServiceError ->
                    GetTicketFeedUpdate(error = UpdateError.WebService)

                else ->
                    GetTicketFeedUpdate(response.responseData)
            }
        }
    }

    override fun getTickets(): LiveData<GetTicketsUpdate> {
        return Transformations.map(webService.getTickets(RequestBase())){ response ->
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

    override fun addComment(ticketId: Int, comment: String, attachments: List<Attachment>?) {
        val localComment = localDataProvider.newLocalTextComment(comment)
        addComment(ticketId, localComment)
    }

    override fun retryComment(ticketId: Int, localComment: Comment) {
        if (!localComment.hasAttachments())
            addComment(ticketId, localComment, false)
        else
            uploadFile(ticketId, localComment, localComment.attachments!![0].uri!!)
    }

    override fun createTicket(userName: String, ticket: TicketDescription) {
        webService.createTicket(CreateTicketRequest(userName, ticket))
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
        uploadFile(ticketId, null, fileUri)
    }

    private fun uploadFile(ticketId: Int, localComment: Comment?, fileUri: Uri) {
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
        val isNewComment = localComment == null
        val comment = when{
            isNewComment -> localDataProvider.newLocalAttachmentComment(
                cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)),
                cursor.getInt(cursor.getColumnIndex(SIZE)),
                fileUri)
            else -> localComment!!
        }
        var cancelled = false
        val progressCallback = FileUploadCallbacks().also {
            it.subscribeOnCancel {
                cancelled = true
                notifySubscribers(CommentCancelledUpdate(ticketId, comment))
                it.unsubscribeFromCancel()
            }
        }
        notifySubscribers(CommentAddedUpdate(ticketId, comment, fileUploadCallbacks = progressCallback, isNew = isNewComment))
        webService.uploadFile(
            UploadFileRequest(
                ticketId,
                cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)),
                contentResolver.openInputStream(fileUri),
                progressCallback))
            .observeForever { response ->
                when {
                    cancelled -> return@observeForever
                    response?.status == Status.WebServiceError ->
                        notifySubscribers(CommentAddedUpdate(ticketId, comment, error = UpdateError.WebService))
                    response?.responseData == null ->
                        notifySubscribers(CommentAddedUpdate(ticketId, comment, error = UpdateError.Unknown))
                    else ->
                        addComment(
                            ticketId,
                            localDataProvider.updateLocalToServerAttachment(comment, response.responseData.guid),
                            false)
                }
            }
    }

    private fun addComment(ticketId: Int, localComment: Comment, isNewComment: Boolean = true) {
        notifySubscribers(CommentAddedUpdate(ticketId, localComment, isNewComment))
        webService.addComment(AddCommentRequest(ticketId, localComment.body, localComment.attachments, localComment.author.name))
            .observeForever { response ->
                notifySubscribers(
                    when{
                        response?.status == Status.WebServiceError ->
                            CommentAddedUpdate(ticketId, localComment, error = UpdateError.WebService)
                        response?.responseData == null ->
                            CommentAddedUpdate(ticketId, localComment, error = UpdateError.Unknown)
                        else ->
                            CommentAddedUpdate(
                                response.request.ticketId,
                                localDataProvider.localToServerComment(localComment, response.responseData))
                    }
                )
            }
    }


    private fun notifySubscribers(update: UpdateBase) {
        subscribers[update.type]?.forEach {
            it.onUpdateReceived(update)
        }
    }
}