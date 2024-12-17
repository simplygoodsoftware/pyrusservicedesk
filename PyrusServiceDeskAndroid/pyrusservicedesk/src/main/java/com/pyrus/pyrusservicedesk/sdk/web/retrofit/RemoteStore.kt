package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_1
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.request.UploadFileRequest
import com.pyrus.pyrusservicedesk.sdk.response.*
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.sdk.web.request_body.*
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.getFirstNSymbols
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.UserData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository.Companion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.coroutineContext

private const val FAILED_AUTHORIZATION_ERROR_CODE = 403

/**
 * Web Repository implementation based on [Retrofit] library.
 *
 * @param instanceId UID of app instance. Generated installation id is used by default.
 * @param fileResolver helper for making upload file requests.
 */
@Keep
internal class RemoteStore(
    private val instanceId: String,
    private val fileResolver: FileResolver,
    private val fileManager: FileManager,
    private val api: ServiceDeskApi,
    private val account: Account,
) {

    private val sequentialRequests = LinkedBlockingQueue<SequentialRequest>()

    private val apiFlag = "AAAAAAAAAAAU"

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean): Try<Comments> {
        PLog.d(
            TAG, "getFeed, " +
                "appId: ${account.appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}"
        )
        val commentsTry = api.getTicketFeed(
            GetFeedBody(
                account.appId,
                getUserId(),
                getSecurityKey(),
                instanceId,
                getVersion(),
                keepUnread,
                apiFlag
            )
        )
        PLog.d(TAG, "getFeed, isSuccessful: ${commentsTry.isSuccess()}")
        return commentsTry
    }

    /**
     * Provides available tickets.
     */
    suspend fun getTickets(): Try<List<Ticket>> {
        PLog.d(
            TAG, "getTickets, " +
                "appId: ${account.appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}"
        )
        val ticketsTry = api.getTickets(
            RequestBodyBase(
                appId = account.appId,
                userId = getUserId(),
                securityKey = getSecurityKey(),
                authorId = getInstanceId(),
                version = getVersion(),
                apiSign = TODO(),
                needFullInfo = TODO(),
                additionalUsers = TODO(),
                commands = TODO(),
                authorName = TODO(),
                instanceId = TODO()
            )
        )
        PLog.d(TAG, "getTickets, isSuccessful: ${ticketsTry.isSuccess()}")
        return ticketsTry.map { it.tickets ?: emptyList() }
    }

    suspend fun getAllData(): Try<Tickets> {
        PLog.d(
            TAG, "getAllData, " +
                    "appId: ${account.appId.getFirstNSymbols(10)}, " +
                    "userId: ${getUserId().getFirstNSymbols(10)}, " +
                    "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                    "apiVersion: ${getVersion()}"
        )
        val ticketsTry = api.getTickets(
            RequestBodyBase(
                needFullInfo = true,
                additionalUsers = getAdditionalUsers(),
                authorId = "10",
                authorName = "Kate Test",
                appId = PyrusServiceDesk.users[0].appId,
                userId = PyrusServiceDesk.users[0].userId,
                instanceId = "4F71E6BA-55F8-46EE-B281-C9E18C42224F",
                version = getVersion(),
                apiSign = apiFlag,
                securityKey = getSecurityKey(),
            )
        )
        PLog.d(TAG, "getTickets, isSuccessful: ${ticketsTry.isSuccess()}")
        return ticketsTry.map { it }
    }

    private fun getAdditionalUsers(): List<UserData> {
        val list = PyrusServiceDesk.users.map { UserData(it.appId, it.userId, getSecurityKey()?:"") }
        return list
    }


    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHooks is used for posting progress as well as checking cancellation signal.
     */
    suspend fun addFeedComment(
        comment: Comment,
        uploadFileHooks: UploadFileHooks?
    ): Try<AddCommentResponseData> {
        return addComment(EMPTY_TICKET_ID, comment, uploadFileHooks)
    }

    /**
     * Registers the given push [token].
     * @param token if null push notifications stop.
     * @param tokenType cloud messaging type.
     */
    suspend fun setPushToken(token: String?, tokenType: String): Try<Unit> {
        PLog.d(
            TAG, "setPushToken, " +
                "appId: ${account.appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}, " +
                "token: $token, " +
                "tokenType: $tokenType"
        )

        val setPushTokenTry = api.setPushToken(
            SetPushTokenBody(
                account.appId,
                getUserId(),
                getSecurityKey(),
                getInstanceId(),
                getVersion(),
                token,
                tokenType
            )
        )

        return setPushTokenTry.map { }
    }

    private suspend fun addComment(
        ticketId: Int,
        comment: Comment,
        uploadFileHooks: UploadFileHooks?,
    ): Try<AddCommentResponseData> {

        PLog.d(
            TAG, "addComment, " +
                "appId: ${account.appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}, " +
                "ticketId: $ticketId"
        )

        val request = CommentRequest()
        sequentialRequests.offer(request)

        var cament = comment
        if (cament.hasAttachments()) {
            val uploadAttachmentsTry = uploadAttachments(cament.attachments!!, uploadFileHooks)
            if (!uploadAttachmentsTry.isSuccess()) {
                return uploadAttachmentsTry
            }
            cament = cament.applyNewAttachments(uploadAttachmentsTry.value)
        }
        val addFeedCommentTry = api.addFeedComment(
            AddCommentRequestBody(
                account.appId,
                getUserId(),
                getSecurityKey(),
                getInstanceId(),
                getVersion(),
                cament.body,
                cament.attachments,
                ConfigUtils.getUserName(),
                cament.rating,
                StaticRepository.EXTRA_FIELDS
            )
        )
        PLog.d(TAG, "addComment, isSuccessful: ${addFeedCommentTry.isSuccess()}}")
        return addFeedCommentTry
    }

    @Throws(Exception::class)
    private suspend fun uploadAttachments(
        attachments: List<Attachment>,
        uploadFileHooks: UploadFileHooks?
    ): Try<List<Attachment>> {
        val uploadResponses = ArrayList<FileUploadResponseData>(attachments.size)

        for (attachment in attachments) {
            val localUri = attachment.localUri ?: throw Exception()
            val uploadData = fileResolver.getUploadFileData(localUri) ?: throw Exception()
            val uploadFileTry = uploadFile(UploadFileRequest(uploadData, uploadFileHooks))
            if (!uploadFileTry.isSuccess()) {
                return uploadFileTry
            }
//            uploadResponses += uploadFile(UploadFileRequest(uploadData, uploadFileHooks))
        }
        if (uploadFileHooks?.isCancelled == true) {
            throw Exception()
        }
        val newAttachments = mutableListOf<Attachment>()
        for (i in uploadResponses.indices) {
            val response = uploadResponses[i]
            val oldAttachment = attachments[i]

//            if (response.responseError != null || response.result == null) {
//                throw Exception()
//            }

//            newAttachments.add(oldAttachment.toRemoteAttachment(response.result.guid))
            fileManager.removeFile(oldAttachment.localUri)
        }
//        return newAttachments
        return TODO()
    }

    private suspend fun uploadFile(request: UploadFileRequest): Try<FileUploadResponseData> {

        val uploadFileTry = api.uploadFile(
            UploadFileRequestBody(
                request.fileUploadRequestData.fileName,
                request.fileUploadRequestData.fileInputStream,
                request.uploadFileHooks,
                coroutineContext
            ).toMultipartBody()
        )

        return uploadFileTry
    }

    private fun getUserId() = when(account) {
        is Account.V1 -> instanceId
        is Account.V2 -> account.userId
    }

    private fun getVersion(): Int = when (account) {
        is Account.V1 -> API_VERSION_2 //TODO
        is Account.V2 -> API_VERSION_2
    }

    private fun getSecurityKey() = when (account) {
        is Account.V1 -> null
        is Account.V2 -> account.securityKey
    }

    private fun getInstanceId() = when (API_VERSION_2) {
        getVersion() -> instanceId
        else -> null
    }

    private fun Attachment.toRemoteAttachment(guid: String) = Attachment(
        id,
        guid,
        type,
        name,
        bytesSize,
        isText,
        isVideo,
        localUri
    )

    private fun AddCommentResponseData.applyAttachments(
        attachments: List<Attachment>?
    ): AddCommentResponseData {

        if (!mustBeConvertedToRemoteAttachments(attachments, attachmentIds)) {
            return this
        }
        return AddCommentResponseData(
            commentId,
            attachmentIds,
            applyRemoteIdsToAttachments(attachments!!, attachmentIds!!)
        )
    }

    private fun applyRemoteIdsToAttachments(
        sentAttachments: List<Attachment>,
        remoteAttachmentIds: List<Int>,
    ): List<Attachment> {
        val newAttachmentsList = mutableListOf<Attachment>()
        sentAttachments.forEachIndexed { index, attachment ->
            newAttachmentsList.add(attachment.withRemoteId(remoteAttachmentIds[index]))
        }
        return newAttachmentsList
    }

    private fun Attachment.withRemoteId(remoteId: Int) = Attachment(
        remoteId,
        guid,
        type,
        name,
        bytesSize,
        isText,
        isVideo,
        localUri
    )

    private fun mustBeConvertedToRemoteAttachments(
        sentAttachments: List<Attachment>?,
        remoteAttachmentIds: List<Int>?,
    ): Boolean {

        return !sentAttachments.isNullOrEmpty()
            && !remoteAttachmentIds.isNullOrEmpty()
            && sentAttachments.size == remoteAttachmentIds.size
    }

    private fun Comment.applyNewAttachments(newAttachments: List<Attachment>): Comment {
        return Comment(commentId, body, isInbound, newAttachments, creationDate, author, localId)
    }

    private fun <T> createError(response: Response<T>): ResponseError {
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

    companion object {
        private val TAG = RemoteStore::class.java.simpleName
    }

}


private interface SequentialRequest


private class CommentRequest : SequentialRequest