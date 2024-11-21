package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk.log.PLog
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
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.Try
import com.pyrus.pyrusservicedesk.utils.getFirstNSymbols
import com.pyrus.pyrusservicedesk.utils.isSuccess
import com.pyrus.pyrusservicedesk.utils.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.coroutineContext

private const val FAILED_AUTHORIZATION_ERROR_CODE = 403

/**
 * Web Repository implementation based on [Retrofit] library.
 *
 * @param appId id of the app that obtained through special Pyrus form.
 * @param instanceId UID of app instance. Generated installation id is used by default.
 * @param fileResolver helper for making upload file requests.
 */
@Keep
internal class RemoteStore(
    private val appId: String,
    private val instanceId: String,
    private val fileResolver: FileResolver,
    private val fileManager: FileManager,
    private val api: ServiceDeskApi,
) {

    private val sequentialRequests = LinkedBlockingQueue<SequentialRequest>()

    private val apiFlag = "AAAAAAAAAAAU"

    /**
     * Provides tickets in single feed representation.
     */
    suspend fun getFeed(keepUnread: Boolean): Try<Comments> {
        PLog.d(
            TAG, "getFeed, " +
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}"
        )
        val commentsTry = api.getTicketFeed(
            GetFeedBody(
                appId,
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
    suspend fun getTickets(): Try<List<TicketShortDescription>> {
        PLog.d(
            TAG, "getTickets, " +
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}"
        )
        val ticketsTry = api.getTickets(
            RequestBodyBase(
                appId,
                getUserId(),
                getSecurityKey(),
                getInstanceId(),
                getVersion()
            )
        )
        PLog.d(TAG, "getTickets, isSuccessful: ${ticketsTry.isSuccess()}")
        return ticketsTry.map { it.tickets }
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
                "appId: ${appId.getFirstNSymbols(10)}, " +
                "userId: ${getUserId().getFirstNSymbols(10)}, " +
                "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                "apiVersion: ${getVersion()}, " +
                "token: $token, " +
                "tokenType: $tokenType"
        )

        val setPushTokenTry = api.setPushToken(
            SetPushTokenBody(
                appId,
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
                "appId: ${appId.getFirstNSymbols(10)}, " +
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
                appId,
                getUserId(),
                getSecurityKey(),
                getInstanceId(),
                getVersion(),
                cament.body,
                cament.attachments,
                ConfigUtils.getUserName(),
                cament.rating
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

    companion object {
        private val TAG = RemoteStore::class.java.simpleName
    }

}


private interface SequentialRequest


private class CommentRequest : SequentialRequest