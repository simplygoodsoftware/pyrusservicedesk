package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.google.gson.Gson
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk.log.PLog
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.general.RemoteRepository
import com.pyrus.pyrusservicedesk.sdk.request.UploadFileRequest
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.request_body.*
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getBaseUrl
import com.pyrus.pyrusservicedesk.utils.getFirstNSymbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Web [GeneralRepository] implementation based on [Retrofit] library.
 *
 * @param appId id of the app that obtained through special Pyrus form.
 * @param instanceId UID of app instance. Generated installation id is used by default.
 * @param fileResolver helper for making upload file requests.
 */
@Keep
internal class RetrofitWebRepository(
    private val appId: String,
    private val instanceId: String,
    private val fileResolver: FileResolver,
    private val fileManager: FileManager,
    domain: String?,
    gson: Gson
) : RemoteRepository {

    private val api: ServiceDeskApi

    private val sequentialRequests = LinkedBlockingQueue<SequentialRequest>()

    init {
        val httpBuilder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

        val retrofit = Retrofit.Builder()
                .baseUrl(getBaseUrl(domain))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpBuilder.build())
                .build()

        api = retrofit.create(ServiceDeskApi::class.java)
    }

    override suspend fun getFeed(keepUnread: Boolean): Response<Comments> {
        PLog.d(TAG, "getFeed, " +
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}"
        )
        return withContext<Response<Comments>>(Dispatchers.IO){
            try {
                api.getTicketFeed(GetFeedBody(appId, getUserId(), getSecurityKey(), instanceId, getVersion(), keepUnread)).execute().run {
                    PLog.d(TAG, "getFeed, isSuccessful: $isSuccessful, body() != null: ${body() != null}")
                    when {
                        isSuccessful && body() != null -> ResponseImpl.success(body()!!)
                        else -> ResponseImpl.failure(createError(this))
                    }
                }

            } catch (ex: Exception) {
                ResponseImpl.failure(NoInternetConnection("No internet connection"))
            }
        }
    }

    override suspend fun getTickets(): GetTicketsResponse {
        PLog.d(TAG, "getTickets, " +
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}"
        )
        return withContext(Dispatchers.IO){
            try {
                api.getTickets(RequestBodyBase(appId, getUserId(), getSecurityKey(), getInstanceId(), getVersion())).execute().run {
                    PLog.d(TAG, "getTickets, isSuccessful: $isSuccessful, body() != null: ${body() != null}")
                    when {
                        isSuccessful && body() != null -> GetTicketsResponse(tickets = body()!!.tickets)
                        else -> GetTicketsResponse(createError(this))
                    }
                }
            } catch (ex: Exception) {
                GetTicketsResponse(NoInternetConnection("No internet connection"))
            }
        }
    }

    override suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks?): Response<AddCommentResponseData> {
        return addComment(EMPTY_TICKET_ID, comment, uploadFileHooks)
    }

    override suspend fun setPushToken(token: String?): SetPushTokenResponse {
        PLog.d(TAG, "setPushToken, " +
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}, " +
                "token: $token"
        )
        return withContext(Dispatchers.IO){
            try {
                api.setPushToken(
                    SetPushTokenBody(
                        appId,
                        getUserId(),
                        getSecurityKey(),
                        getInstanceId(),
                        getVersion(),
                        token
                    )
                ).execute().run {
                    PLog.d(TAG, "setPushToken, isSuccessful: $isSuccessful, body() != null: ${body() != null}")
                    when {
                        isSuccessful -> SetPushTokenResponse()
                        else -> SetPushTokenResponse(createError(this))
                    }
                }
            } catch (ex: Exception) {
                SetPushTokenResponse(NoInternetConnection("No internet connection"))
            }
        }
    }

    private fun getUserId(): String {
        if (getVersion() == API_VERSION_2) {
            return PyrusServiceDesk.get().userId ?: instanceId
        }
        return instanceId
    }

    private fun getVersion(): Int {
        return PyrusServiceDesk.get().apiVersion
    }

    private fun getSecurityKey(): String? {
        if (getVersion() == API_VERSION_2)
            return PyrusServiceDesk.get().securityKey
        return null
    }

    private fun getInstanceId(): String? {
        if (getVersion() == API_VERSION_2)
            return instanceId
        return null
    }

    private suspend fun addComment(
        ticketId: Int,
        comment: Comment,
        uploadFileHooks: UploadFileHooks?
    ): Response<AddCommentResponseData> {

        PLog.d(TAG, "addComment, " +
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}, " +
                "ticketId: $ticketId"
        )

        val request = CommentRequest()
        sequentialRequests.offer(request)

        return withContext<Response<AddCommentResponseData>>(PyrusServiceDesk.DISPATCHER_IO_SINGLE) {
            var cament = comment
            if (cament.hasAttachments()) {
                val newAttachments =
                    try {
                        cament.attachments!!.upload(uploadFileHooks)
                    } catch (ex: Exception) {
                        sequentialRequests.poll()
                        return@withContext ResponseImpl.failure(ApiCallError(ex.message ?: "Error while uploading files"))
                    }
                cament = cament.applyNewAttachments(newAttachments)
            }

            val call = api.addFeedComment(AddCommentRequestBody(
                appId,
                getUserId(),
                getSecurityKey(),
                getInstanceId(),
                getVersion(),
                cament.body,
                cament.attachments,
                ConfigUtils.getUserName(),
                cament.rating))
            return@withContext try {
                call
                    .execute()
                    .run {
                        PLog.d(TAG, "addComment, isSuccessful: $isSuccessful, body() != null: ${body() != null}")
                        when {
                            isSuccessful && body() != null -> {
                                val data = when{
                                    body()!!.attachmentIds.isNullOrEmpty() -> body()
                                    else -> body()!!.applyAttachments(cament.attachments)
                                }
                                ResponseImpl.success(data!!)
                            }
                            else -> ResponseImpl.failure(createError(this))
                        }
                    }
            }
            catch (ex: Exception){
                ResponseImpl.failure(NoInternetConnection("No internet connection"))
            }
            finally {
                sequentialRequests.poll()
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun List<Attachment>.upload(uploadFileHooks: UploadFileHooks?): List<Attachment> {
        val uploadResponses = fold(ArrayList<UploadFileResponse>(size))
        { responses, attachment ->
            if (attachment.localUri == null)
                throw Exception()
            with(fileResolver.getUploadFileData(attachment.localUri)) {
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

            if (uploadFileResponse.responseError != null || uploadFileResponse.result == null)
                throw Exception()

            newAttachments.add(get(index).toRemoteAttachment(uploadFileResponse.result.guid))
            fileManager.removeFile(get(index).localUri)
        }
        return newAttachments
    }

    private suspend fun uploadFile(request: UploadFileRequest): UploadFileResponse {
        return try {
            api.uploadFile(
                UploadFileRequestBody(
                    request.fileUploadRequestData.fileName,
                    request.fileUploadRequestData.fileInputStream,
                    request.uploadFileHooks,
                    coroutineContext
                )
                    .toMultipartBody()
            )
                .execute()
                .run {
                    when {
                        isSuccessful && body() != null -> UploadFileResponse(uploadData = body())
                        else -> UploadFileResponse(createError(this))
                    }
                }
        } catch (ex: Exception) {
            UploadFileResponse(NoInternetConnection("No internet connection"))
        }
    }

    companion object {
        private val TAG = RetrofitWebRepository::class.java.simpleName
    }

}

private const val FAILED_AUTHORIZATION_ERROR_CODE = 403

private fun <T> createError(response: retrofit2.Response<T>): ResponseError {
    return when (FAILED_AUTHORIZATION_ERROR_CODE) {
        response.code() -> {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    PyrusServiceDesk.onAuthorizationFailed?.run()
                }
            }
            AuthorizationError(response.message())
        }
        else -> ApiCallError(response.message())
    }
}

private fun AddCommentResponseData.applyAttachments(attachments: List<Attachment>?): AddCommentResponseData {
    if (!mustBeConvertedToRemoteAttachments(attachments, attachmentIds))
        return this
    return AddCommentResponseData(commentId, attachmentIds, applyRemoteIdsToAttachments(attachments!!, attachmentIds!!))
}

private fun applyRemoteIdsToAttachments(sentAttachments: List<Attachment>, remoteAttachmentIds: List<Int>): List<Attachment> {
    val newAttachmentsList = mutableListOf<Attachment>()
    sentAttachments.forEachIndexed { index, attachment ->
        newAttachmentsList.add(attachment.withRemoteId(remoteAttachmentIds[index]))
    }
    return newAttachmentsList
}

private fun mustBeConvertedToRemoteAttachments(sentAttachments: List<Attachment>?,
                                               remoteAttachmentIds: List<Int>?): Boolean {

    return !sentAttachments.isNullOrEmpty()
            && !remoteAttachmentIds.isNullOrEmpty()
            && sentAttachments.size == remoteAttachmentIds.size
}

private fun Comment.applyNewAttachments(newAttachments: List<Attachment>): Comment {
    return Comment(commentId, body, isInbound, newAttachments, creationDate, author, localId)
}

private fun Attachment.toRemoteAttachment(guid: String) = Attachment(id, guid, type, name, bytesSize, isText, isVideo, localUri)
private fun Attachment.withRemoteId(remoteId: Int) = Attachment(remoteId, guid, type, name, bytesSize, isText, isVideo, localUri)


private interface SequentialRequest
private class CommentRequest : SequentialRequest