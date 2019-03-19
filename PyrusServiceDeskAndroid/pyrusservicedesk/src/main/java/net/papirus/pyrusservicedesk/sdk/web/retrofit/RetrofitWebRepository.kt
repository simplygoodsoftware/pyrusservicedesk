package net.papirus.pyrusservicedesk.sdk.web.retrofit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.papirus.pyrusservicedesk.sdk.BASE_URL
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
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


internal class RetrofitWebRepository(
        private val appId: String,
        private val userId: String,
        private val userName: String,
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

    override suspend fun getConversation(): GetConversationResponse {
        return api.getConversation(RequestBodyBase(appId, userId)).execute().run {
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

    override suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks): AddCommentResponse {
        var cament = comment
        if (cament.hasAttachments()) {
            val newAttachments =
                try {
                    cament.attachments!!.upload(uploadFileHooks)
                } catch (ex: Exception) {
                    return AddCommentResponse(ResponseError.WebServiceError)
                }
            cament = cament.applyNewAttachments(newAttachments)
        }
        return api.addComment(
            AddCommentRequestBody(appId, userId, cament.body, cament.attachments), ticketId)
            .execute()
            .run {
                when {
                    isSuccessful && body() != null -> AddCommentResponse(
                        commentId = Gson().fromJson<Map<String, Double>>(
                            body()?.string(),
                            Map::class.java)
                            .values
                            .first()
                            .toInt())
                else -> AddCommentResponse(ResponseError.WebServiceError)
            }
        }
    }

    override suspend fun createTicket(
        description: TicketDescription,
        uploadFileHooks: UploadFileHooks
    ): CreateTicketResponse {
        var descr = description
        if (descr.hasAttachments()) {
            val newAttachments =
                try {
                    descr.attachments!!.upload(uploadFileHooks)
                } catch (ex: Exception) {
                    return CreateTicketResponse(ResponseError.WebServiceError)
                }

            descr = descr.applyNewAttachments(newAttachments)
        }
        return api.createTicket(
            CreateTicketRequestBody(
                appId,
                userId,
                userName,
                descr))
            .execute()
            .run {
                when {
                    isSuccessful && body() != null -> CreateTicketResponse(
                            ticketId = Gson().fromJson<Map<String, Int>>(
                                body()?.string(),
                                Map::class.java)
                                .values
                                .first()
                                .toInt()
                        )
                    else -> CreateTicketResponse(ResponseError.WebServiceError)
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
                    isSuccessful && body() != null -> UploadFileResponse(uploadData = body())
                    else -> UploadFileResponse(ResponseError.WebServiceError)
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

            if (uploadFileResponse.error != null || uploadFileResponse.result == null)
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
