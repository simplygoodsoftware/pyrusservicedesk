package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.google.gson.GsonBuilder
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CreateTicketResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.request.UploadFileRequest
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.request_body.*
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.ISO_DATE_PATTERN
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Web [GeneralRepository] implementation based on [Retrofit] library.
 *
 * @param appId id of the app that obtained through special Pyrus form.
 * @param userId UID of user. Generated installation id is used by default.
 * @param fileResolver helper for making upload file requests.
 */
internal class RetrofitWebRepository(
        private val appId: String,
        private val userId: String,
        private val fileResolver: FileResolver)
    : GeneralRepository {

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

    override suspend fun getFeed(): GetFeedResponse {
        return withContext(Dispatchers.IO){
            try {
                api.getTicketFeed(RequestBodyBase(appId, userId)).execute().run {
                    when {
                        isSuccessful && body() != null -> GetFeedResponse(comments = body()!!.comments)
                        else -> GetFeedResponse(ApiCallError(this.message()))
                    }
                }
            } catch (ex: Exception) {
                GetFeedResponse(NoInternetConnection("No internet connection"))
            }
        }
    }

    override suspend fun getTickets(): GetTicketsResponse {
        return withContext(Dispatchers.IO){
            try {
                api.getTickets(RequestBodyBase(appId, userId)).execute().run {
                    when {
                        isSuccessful && body() != null -> GetTicketsResponse(tickets = body()!!.tickets)
                        else -> GetTicketsResponse(ApiCallError(this.message()))
                    }
                }
            } catch (ex: Exception) {
                GetTicketsResponse(NoInternetConnection("No internet connection"))
            }
        }
    }

    override suspend fun getTicket(ticketId: Int): GetTicketResponse {
        return withContext(Dispatchers.IO){
            try {
                api.getTicket(RequestBodyBase(appId, userId), ticketId).execute().run {
                    when {
                        isSuccessful && body() != null -> GetTicketResponse(ticket = body())
                        else -> GetTicketResponse(ApiCallError(this.message()))
                    }
                }
            } catch (ex: Exception) {
                GetTicketResponse(NoInternetConnection("No internet connection"))
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
                        sequentialRequests.poll()
                        return@withContext CreateTicketResponse(ApiCallError(ex.message ?: "Error while uploading files"))
                    }

                descr = descr.applyNewAttachments(newAttachments)
            }
            return@withContext try {
                api.createTicket(
                    CreateTicketRequestBody(
                        appId,
                        userId,
                        ConfigUtils.getUserName(),
                        descr
                    )
                )
                    .execute()
                    .run {
                        sequentialRequests.poll()
                        when {
                            isSuccessful && body() != null -> {
                                with(sequentialRequests.iterator()) {
                                    while (hasNext()) {
                                        val element = next()
                                        if (element !is CommentRequest || element.ticketId != EMPTY_TICKET_ID)
                                            break
                                        element.ticketId = body()!!.ticketId
                                    }
                                }
                                val data = when{
                                    body()!!.attachmentIds.isNullOrEmpty() -> body()
                                    else -> body()!!.applyAttachments(descr.attachments)
                                }

                                CreateTicketResponse(data = data)
                            }
                            else -> CreateTicketResponse(ApiCallError(this.message()))
                        }
                    }
            } catch (ex: Exception) {
                sequentialRequests.poll()
                CreateTicketResponse(NoInternetConnection("No internet connection"))
            }
        }
    }

    override suspend fun setPushToken(token: String): SetPushTokenResponse {
        return withContext(Dispatchers.IO){
            try {
                api.setPushToken(SetPushTokenBody(appId, userId, token)).execute().run {
                    when {
                        isSuccessful -> SetPushTokenResponse()
                        else -> SetPushTokenResponse(ApiCallError(this.message()))
                    }
                }
            } catch (ex: Exception) {
                SetPushTokenResponse(NoInternetConnection("No internet connection"))
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
                        sequentialRequests.poll()
                        return@withContext AddCommentResponse(ApiCallError(ex.message ?: "Error while uploading files"))
                    }
                cament = cament.applyNewAttachments(newAttachments)
            }

            val call = when {
                isFeed -> api.addFeedComment(AddCommentRequestBody(appId, userId, cament.body, cament.attachments, ConfigUtils.getUserName()))
                else -> api.addComment(
                    AddCommentRequestBody(appId, userId, cament.body, cament.attachments, ConfigUtils.getUserName()),
                    request.ticketId)
            }
            return@withContext try {
                call
                    .execute()
                    .run {
                        when {
                            isSuccessful && body() != null -> {
                                val data = when{
                                    body()!!.attachmentIds.isNullOrEmpty() -> body()
                                    else -> body()!!.applyAttachments(cament.attachments)
                                }
                                AddCommentResponse(commentData = data)
                            }
                            else -> AddCommentResponse(ApiCallError(this.message()))
                        }
                    }
            }
            catch (ex: Exception){
                AddCommentResponse(NoInternetConnection("No internet connection"))
            }
            finally {
                sequentialRequests.poll()
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
        if(uploadFileHooks?.isCancelled == true)
            throw Exception()
        val newAttachments = mutableListOf<Attachment>()
        uploadResponses.forEachIndexed { index, uploadFileResponse ->

            if (uploadFileResponse.error != null || uploadFileResponse.result == null)
                throw Exception()

            newAttachments.add(get(index).toRemoteAttachment(uploadFileResponse.result.guid))
        }
        return newAttachments
    }

    private fun uploadFile(request: UploadFileRequest): UploadFileResponse {
        return try {
            api.uploadFile(
                UploadFileRequestBody(
                    request.fileUploadRequestData.fileName,
                    request.fileUploadRequestData.fileInputStream,
                    request.uploadFileHooks
                )
                    .toMultipartBody()
            )
                .execute()
                .run {
                    when {
                        isSuccessful && body() != null -> UploadFileResponse(uploadData = body())
                        else -> UploadFileResponse(ApiCallError(this.message()))
                    }
                }
        } catch (ex: Exception) {
            UploadFileResponse(NoInternetConnection("No internet connection"))
        }
    }
}

private fun CreateTicketResponseData.applyAttachments(attachments: List<Attachment>?): CreateTicketResponseData {
    if (attachments.isNullOrEmpty() || this.attachmentIds.isNullOrEmpty() || attachments.size != this.attachmentIds.size)
        return this
    val newAttachmentsList = mutableListOf<Attachment>()
    attachments.forEachIndexed { index, attachment ->
        newAttachmentsList.add(attachment.withRemoteId(attachmentIds[index]))
    }
    return CreateTicketResponseData(ticketId, attachmentIds, newAttachmentsList)
}

private fun AddCommentResponseData.applyAttachments(attachments: List<Attachment>?): AddCommentResponseData {
    if (attachments.isNullOrEmpty() || this.attachmentIds.isNullOrEmpty() || attachments.size != this.attachmentIds.size)
        return this
    val newAttachmentsList = mutableListOf<Attachment>()
    attachments.forEachIndexed { index, attachment ->
        newAttachmentsList.add(attachment.withRemoteId(attachmentIds[index]))
    }
    return AddCommentResponseData(commentId, attachmentIds, newAttachmentsList)
}

private fun TicketDescription.applyNewAttachments(newAttachments: List<Attachment>): TicketDescription {
    return TicketDescription(subject, description, newAttachments)
}

private fun Comment.applyNewAttachments(newAttachments: List<Attachment>): Comment {
    return Comment(commentId, body, isInbound, newAttachments, creationDate, author, localId)
}

private fun Attachment.toRemoteAttachment(guid: String) = Attachment(id, guid, type, name, bytesSize, isText, isVideo, uri)
private fun Attachment.withRemoteId(remoteId: Int) = Attachment(remoteId, guid, type, name, bytesSize, isText, isVideo, uri)


private interface SequentialRequest
private class CommentRequest(var ticketId: Int): SequentialRequest
private class CreateTicketRequest: SequentialRequest
