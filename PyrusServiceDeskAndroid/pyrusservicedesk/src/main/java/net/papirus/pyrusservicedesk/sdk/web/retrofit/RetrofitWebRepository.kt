package net.papirus.pyrusservicedesk.sdk.web.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.withContext
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.BASE_URL
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.request.UploadFileRequest
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks
import net.papirus.pyrusservicedesk.sdk.web.request_body.AddCommentRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.CreateTicketRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import net.papirus.pyrusservicedesk.sdk.web.request_body.UploadFileRequestBody
import net.papirus.pyrusservicedesk.utils.ISO_DATE_PATTERN
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


internal class RetrofitWebRepository(
        private val appId: String,
        private val userId: String,
        private val userName: String,
        private val fileResolver: FileResolver)
    : Repository {

    private val api: ServiceDeskApi

    private val sequentialRequests = LinkedBlockingQueue<SequentialRequest>()

    init {
        val httpBuilder = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)

        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().setDateFormat(ISO_DATE_PATTERN).create()))
                .client(httpBuilder.build())
                .build()

        api = retrofit.create(ServiceDeskApi::class.java)
    }

    override suspend fun getFeed(): GetConversationResponse {
        return api.getTicketFeed(RequestBodyBase(appId, userId)).execute().run {
            when {
                isSuccessful && body() != null -> GetConversationResponse(comments = body()!!.comments)
                else -> GetConversationResponse(ResponseError.WebServiceError)
            }
        }
    }

    override suspend fun getTickets(): GetTicketsResponse {
        return api.getTickets(RequestBodyBase(appId, userId)).execute().run {
            when {
                isSuccessful && body() != null -> GetTicketsResponse(tickets =  body()!!.tickets)
                else -> GetTicketsResponse(ResponseError.WebServiceError)
            }
        }
    }

    override suspend fun getTicket(ticketId: Int): GetTicketResponse {
        return api.getTicket(RequestBodyBase(appId, userId), ticketId).execute().run {
            when {
                isSuccessful && body() != null -> GetTicketResponse(ticket =  body())
                else -> GetTicketResponse(ResponseError.WebServiceError)
            }
        }
    }

    override suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks?)
            : AddCommentResponse {

        return addComment(false, ticketId, comment, uploadFileHooks)
    }

    override suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks?): AddCommentResponse {
        return addComment(true, EMPTY_TICKET_ID, comment, uploadFileHooks)
    }

    override suspend fun createTicket(description: TicketDescription, uploadFileHooks: UploadFileHooks?)
            : CreateTicketResponse {

        sequentialRequests.offer(CreateTicketRequest())

        return withContext(PyrusServiceDesk.DISPATCHER_IO_SINGLE) {
            var descr = description
            if (descr.hasAttachments()) {
                val newAttachments =
                    try {
                        descr.attachments!!.upload(uploadFileHooks)
                    } catch (ex: Exception) {
                        return@withContext CreateTicketResponse(ResponseError.WebServiceError)
                    }

                descr = descr.applyNewAttachments(newAttachments)
            }
            return@withContext api.createTicket(
                CreateTicketRequestBody(
                    appId,
                    userId,
                    userName,
                    descr)
            )
                .execute()
                .run {
                    sequentialRequests.poll()
                    when {
                        isSuccessful && body() != null -> {
                            val ticketId =
                                Gson().fromJson<Map<String, Int>>(
                                    body()?.string(),
                                    Map::class.java)
                                    .values.first().toInt()

                            with(sequentialRequests.iterator()) {
                                while (hasNext()) {
                                    val element = next() as? CommentRequest ?: break
                                    element.ticketId = ticketId
                                }
                            }

                            CreateTicketResponse(ticketId = ticketId)
                        }
                        else -> CreateTicketResponse(ResponseError.WebServiceError)
                    }
                }
        }
    }

    private suspend fun addComment(isFeed: Boolean,
                                   ticketId: Int,
                                   comment: Comment,
                                   uploadFileHooks: UploadFileHooks?): AddCommentResponse {

        val request = CommentRequest(ticketId)
        sequentialRequests.offer(request)

        return withContext(PyrusServiceDesk.DISPATCHER_IO_SINGLE) {
            var cament = comment
            if (cament.hasAttachments()) {
                val newAttachments =
                    try {
                        cament.attachments!!.upload(uploadFileHooks)
                    } catch (ex: Exception) {
                        return@withContext AddCommentResponse(ResponseError.WebServiceError)
                    }
                cament = cament.applyNewAttachments(newAttachments)
            }

            val call = when {
                isFeed -> api.addFeedComment(AddCommentRequestBody(appId, userId, cament.body, cament.attachments, userName))
                else -> api.addComment(
                    AddCommentRequestBody(appId, userId, cament.body, cament.attachments, userName),
                    request.ticketId)
            }
            return@withContext call
                .execute()
                .run {
                    sequentialRequests.poll()
                    when {
                        isSuccessful && body() != null -> AddCommentResponse(
                            commentId = Gson().fromJson<Map<String, Double>>(
                                body()?.string(),
                                Map::class.java
                            )
                                .values.first().toInt()
                        )
                        else -> AddCommentResponse(ResponseError.WebServiceError)
                    }
                }
        }
    }

    @Throws(Exception::class)
    private fun List<Attachment>.upload(uploadFileHooks: UploadFileHooks?): List<Attachment> {
        val uploadResponses = fold(ArrayList<UploadFileResponse>(size))
        { responses, attachment ->
            if (attachment.uri == null)
                throw Exception()
            with(fileResolver.getUploadFileData(attachment.uri)) {
                when (this) {
                    null -> throw Exception()
                    else -> responses.add(
                        uploadFile(
                            UploadFileRequest(
                                this,
                                uploadFileHooks)
                        )
                    )
                }
            }
            responses
        }
        val newAttachments = mutableListOf<Attachment>()
        uploadResponses.forEachIndexed { index, uploadFileResponse ->

            if (uploadFileResponse.error != null || uploadFileResponse.result == null)
                throw Exception()

            newAttachments.add(get(index).toRemoteAttachment(uploadFileResponse.result.guid))
        }
        return newAttachments
    }

    private fun uploadFile(request: UploadFileRequest): UploadFileResponse {
        return api.uploadFile(
            UploadFileRequestBody(
                request.fileUploadRequestData.fileName,
                request.fileUploadRequestData.fileInputStream,
                request.uploadFileHooks)
                .toMultipartBody())
            .execute()
            .run {
                when{
                    isSuccessful && body() != null -> UploadFileResponse(uploadData = body())
                    else -> UploadFileResponse(ResponseError.WebServiceError)
                }
            }
    }
}

private fun TicketDescription.applyNewAttachments(newAttachments: List<Attachment>): TicketDescription {
    return TicketDescription(subject, description, newAttachments)
}

private fun Comment.applyNewAttachments(newAttachments: List<Attachment>): Comment {
    return Comment(commentId, body, isInbound, newAttachments, creationDate, author, localId)
}

private fun Attachment.toRemoteAttachment(guid: String) = Attachment(id, guid, type, name, bytesSize, isText, isVideo, uri)


private interface SequentialRequest
private class CommentRequest(var ticketId: Int): SequentialRequest
private class CreateTicketRequest: SequentialRequest
