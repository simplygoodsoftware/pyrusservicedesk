package net.papirus.pyrusservicedesk.sdk.web.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.papirus.pyrusservicedesk.sdk.BASE_URL
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.ResponseStatus
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.request.AddCommentRequest1
import net.papirus.pyrusservicedesk.sdk.request.CreateTicketRequest1
import net.papirus.pyrusservicedesk.sdk.request.UploadFileRequest
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks
import net.papirus.pyrusservicedesk.sdk.web.request_body.AddCommentRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.CreateTicketRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import net.papirus.pyrusservicedesk.sdk.web.request_body.UploadFileRequestBody
import net.papirus.pyrusservicedesk.utils.ISO_DATE_PATTERN
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors


internal class RetrofitWebService(
        private val appId: String,
        private val userId: String,
        private val fileResolver: FileResolver)
    : Repository {

    private val api: ServiceDeskApi

    init {
        val httpBuilder = OkHttpClient.Builder()
                .dispatcher(Dispatcher(Executors.newSingleThreadExecutor()))

        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().setDateFormat(ISO_DATE_PATTERN).create()))
                .client(httpBuilder.build())
                .build()

        api = retrofit.create(ServiceDeskApi::class.java)
    }

    override fun getConversation(): GetConversationResponse1 {
        return api.getConversation(RequestBodyBase(appId, userId)).execute().run {
            when {
                isSuccessful && body() != null -> GetConversationResponse1(ResponseStatus.Ok, body()!!.comments)
                else -> GetConversationResponse1(ResponseStatus.WebServiceError)
            }
        }
    }

    override fun getTickets(): GetTicketsResponse1 {
        return api.getTickets(RequestBodyBase(appId, userId)).execute().run {
            when {
                isSuccessful && body() != null -> GetTicketsResponse1(ResponseStatus.Ok, body()!!.tickets)
                else -> GetTicketsResponse1(ResponseStatus.WebServiceError)
            }
        }
    }

    override fun getTicket(ticketId: Int): GetTicketResponse1 {
        return api.getTicket(RequestBodyBase(appId, userId), ticketId).execute().run {
            when {
                isSuccessful && body() != null -> GetTicketResponse1(ResponseStatus.Ok, body())
                else -> GetTicketResponse1(ResponseStatus.WebServiceError)
            }
        }
    }

    override fun addComment(request: AddCommentRequest1): AddCommentResponse1 {
        var req = request
        if (request.comment.hasAttachments()) {
            val newAttachments =
                try {
                    req.comment.attachments!!.upload(req.uploadFileHooks)
                } catch (ex: Exception) {
                    return AddCommentResponse1(ResponseStatus.WebServiceError)
                }
            req = AddCommentRequest1(
                req.ticketId,
                req.comment.applyNewAttachments(newAttachments),
                req.uploadFileHooks)
        }
        return api.addComment(
            AddCommentRequestBody(appId, userId, req.comment.body, req.comment.attachments), req.ticketId)
            .execute()
            .run {
                when {
                    isSuccessful && body() != null -> AddCommentResponse1(
                        ResponseStatus.Ok,
                        Gson().fromJson<Map<String, Double>>(
                            body()?.string(),
                            Map::class.java)
                            .values
                            .first()
                            .toInt())
                else -> AddCommentResponse1(ResponseStatus.WebServiceError)
            }
        }
    }

    override fun createTicket(request: CreateTicketRequest1): CreateTicketResponse1 {
        var req = request
        if (req.description.hasAttachments()) {
            val newAttachments =
                try {
                    req.description.attachments!!.upload(req.uploadFileHooks)
                } catch (ex: Exception) {
                    return CreateTicketResponse1(ResponseStatus.WebServiceError)
                }
            req = CreateTicketRequest1(
                req.userName,
                req.description.applyNewAttachments(newAttachments),
                req.uploadFileHooks)
        }
        return api.createTicket(
            CreateTicketRequestBody(
                appId,
                userId,
                req.userName,
                req.description))
            .execute()
            .run {
                when {
                    isSuccessful && body() != null -> CreateTicketResponse1(
                            ResponseStatus.Ok,
                            Gson().fromJson<Map<String, Int>>(
                                body()?.string(),
                                Map::class.java)
                                .values
                                .first()
                                .toInt()
                        )
                    else -> CreateTicketResponse1(ResponseStatus.WebServiceError)
                }
            }
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
                    isSuccessful && body() != null -> UploadFileResponse(ResponseStatus.Ok, body())
                    else -> UploadFileResponse(ResponseStatus.WebServiceError)
                }
            }
    }

    @Throws(Exception::class)
    private fun List<Attachment>.upload(uploadFileHooks: UploadFileHooks): List<Attachment> {
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
                                uploadFileHooks
                            )
                        )
                    )
                }
            }
            responses
        }
        val newAttachments = mutableListOf<Attachment>()
        uploadResponses.forEachIndexed { index, uploadFileResponse ->

            if (uploadFileResponse.status != ResponseStatus.Ok || uploadFileResponse.result == null)
                throw Exception()

            newAttachments.add(get(index).toRemoteAttachment(uploadFileResponse.result.guid))
        }
        return newAttachments
    }
}

private fun TicketDescription.applyNewAttachments(newAttachments: List<Attachment>): TicketDescription {
    return TicketDescription(subject, description, newAttachments)
}

private fun Comment.applyNewAttachments(newAttachments: List<Attachment>): Comment {
    return Comment(commentId, body, isInbound, newAttachments, creationDate, author, localId)
}

private fun Attachment.toRemoteAttachment(guid: String) = Attachment(id, guid, type, name, bytesSize, isText, isVideo, uri)
